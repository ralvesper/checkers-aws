package br.com.highonline.checkers.game.infrastructure.realtime;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.infrastructure.security.GuestTokens;
import br.com.highonline.checkers.game.domain.Game;
import br.com.highonline.checkers.player.domain.PlayerId;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class GameStreamTest {
    @LocalServerPort int port;
    @Autowired GameService service;
    @Autowired RoomService rooms;
    @Autowired GuestTokens tokens;
    @Autowired GameStreamHandler stream;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void sendsConfirmedSnapshotAndReconnectsWithLatestState() throws Exception {
        String white = tokens.issue().token(), black = tokens.issue().token();
        var room = rooms.create(tokens.verify(white));
        Game game = room.game();
        rooms.join(game.id(), tokens.verify(black), room.invitation());
        Messages messages = new Messages();
        WebSocket socket = connect(game, black, messages);
        assertEquals(0, version(messages.next()));
        try {
            String command = "{\"from\":{\"row\":5,\"column\":0},\"landings\":[{\"row\":4,\"column\":1}],\"expectedVersion\":0,\"clientMoveId\":\"" + UUID.randomUUID() + "\"}";
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/games/" + game.id().value() + "/moves"))
                    .header("Authorization", "Bearer " + white).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(command)).build();
            assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(1, version(messages.next()));
            // Rejected stale command must not produce a phantom confirmed snapshot.
            var invalid = HttpRequest.newBuilder(request.uri()).header("Authorization", "Bearer " + white)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(command.replaceFirst("clientMoveId", "unusedId"))).build();
            assertEquals(400, client.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertNull(messages.queue.poll(200, TimeUnit.MILLISECONDS));
        } finally { socket.abort(); }
        Messages reconnected = new Messages();
        WebSocket second = connect(game, black, reconnected);
        try { assertEquals(1, version(reconnected.next())); }
        finally { second.abort(); }
    }

    @Test
    void refusesNonParticipantAndDoesNotLeakOtherGame() throws Exception {
        String white = tokens.issue().token();
        Game first = rooms.create(tokens.verify(white)).game();
        PlayerId whiteSeat = first.whitePlayerId(), blackSeat = first.blackPlayerId();
        var failure = assertThrows(ExecutionException.class, () -> connect(first, tokens.issue().token(), new Messages()));
        assertInstanceOf(WebSocketHandshakeException.class, failure.getCause());
        assertEquals(403, ((WebSocketHandshakeException) failure.getCause()).getResponse().statusCode());
        Messages messages = new Messages();
        WebSocket socket = connect(first, white, messages);
        try {
            assertEquals(0, version(messages.next()));
            Game other = service.create(whiteSeat, blackSeat, whiteSeat);
            Game confirmed = service.play(other.id(), new br.com.highonline.checkers.game.application.PlayMove(
                    whiteSeat, other.legalMoves().get(0), 0, UUID.randomUUID()));
            stream.publish(confirmed);
            assertNull(messages.queue.poll(200, TimeUnit.MILLISECONDS));
        } finally { socket.abort(); }
    }

    private WebSocket connect(Game game, String token, Messages listener) throws Exception {
        return client.newWebSocketBuilder().subprotocols("checkers.v1", "guest." + token).buildAsync(URI.create("ws://localhost:" + port + "/games/"
                + game.id().value() + "/stream"), listener).get(5, TimeUnit.SECONDS);
    }
    private int version(String json) { return JsonPath.read(json, "$.version"); }
    private static class Messages implements WebSocket.Listener {
        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        final StringBuilder buffer = new StringBuilder();
        public void onOpen(WebSocket socket) { socket.request(1); }
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) { queue.add(buffer.toString()); buffer.setLength(0); }
            socket.request(1); return null;
        }
        String next() throws InterruptedException {
            String message = queue.poll(5, TimeUnit.SECONDS); assertNotNull(message); return message;
        }
    }
}
