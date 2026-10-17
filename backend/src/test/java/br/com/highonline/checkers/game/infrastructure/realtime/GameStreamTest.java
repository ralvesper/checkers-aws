package br.com.highonline.checkers.game.infrastructure.realtime;

import br.com.highonline.checkers.game.application.GameService;
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
    @Autowired GameStreamHandler stream;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void sendsConfirmedSnapshotAndReconnectsWithLatestState() throws Exception {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game game = service.create(white, black, white);
        Messages messages = new Messages();
        WebSocket socket = connect(game, black, messages);
        assertEquals(0, version(messages.next()));
        try {
            String command = "{\"from\":{\"row\":5,\"column\":0},\"landings\":[{\"row\":4,\"column\":1}],\"expectedVersion\":0,\"clientMoveId\":\"" + UUID.randomUUID() + "\"}";
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/games/" + game.id().value() + "/moves"))
                    .header("X-Player-Id", white.value().toString()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(command)).build();
            assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(1, version(messages.next()));
            // Rejected stale command must not produce a phantom confirmed snapshot.
            var invalid = HttpRequest.newBuilder(request.uri()).header("X-Player-Id", white.value().toString())
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
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game first = service.create(white, black, white);
        var failure = assertThrows(ExecutionException.class, () -> connect(first, PlayerId.newId(), new Messages()));
        assertInstanceOf(WebSocketHandshakeException.class, failure.getCause());
        assertEquals(403, ((WebSocketHandshakeException) failure.getCause()).getResponse().statusCode());
        Messages messages = new Messages();
        WebSocket socket = connect(first, white, messages);
        try {
            assertEquals(0, version(messages.next()));
            Game other = service.create(white, black, white);
            Game confirmed = service.play(other.id(), new br.com.highonline.checkers.game.application.PlayMove(
                    white, other.legalMoves().get(0), 0, UUID.randomUUID()));
            stream.publish(confirmed);
            assertNull(messages.queue.poll(200, TimeUnit.MILLISECONDS));
        } finally { socket.abort(); }
    }

    private WebSocket connect(Game game, PlayerId actor, Messages listener) throws Exception {
        return client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port + "/games/"
                + game.id().value() + "/stream?playerId=" + actor.value()), listener).get(5, TimeUnit.SECONDS);
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
