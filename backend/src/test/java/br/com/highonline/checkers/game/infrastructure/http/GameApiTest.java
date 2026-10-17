package br.com.highonline.checkers.game.infrastructure.http;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class GameApiTest {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private String owner;
    @BeforeEach void session() throws Exception { owner = issue(); }

    @Test void createReadMoveAndReplay() throws Exception {
        var created = request("POST", "/games", owner, "{}");
        assertEquals(201, created.statusCode());
        String path = created.headers().firstValue("Location").orElseThrow();
        String invitation = value(created, "$.invitation");
        assertEquals(24, (int) value(created, "$.game.pieces.length()"));
        String guest = issue();
        assertEquals(200, request("POST", path + "/join", guest, "{\"invitation\":\"" + invitation + "\"}").statusCode());
        var read = request("GET", path, guest, null);
        assertEquals(200, read.statusCode());
        assertNotEquals(value(created, "$.game.whitePlayerId"), read.headers().firstValue("X-Seat-Id").orElseThrow());
        String body = move(UUID.randomUUID().toString(), 0, 4, 1);
        var accepted = request("POST", path + "/moves", owner, body);
        assertEquals(200, accepted.statusCode());
        assertEquals(1, (int) value(accepted, "$.version"));
        assertEquals(accepted.body(), request("POST", path + "/moves", owner, body).body());
        assertEquals(1, (int) value(request("GET", path, guest, null), "$.version"));
        assertTrue(created.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    }

    @Test void uuidAloneCannotImpersonateEitherParticipant() throws Exception {
        var created = request("POST", "/games", owner, "{}");
        String path = created.headers().firstValue("Location").orElseThrow();
        String whiteId = value(created, "$.game.whitePlayerId"), blackId = value(created, "$.game.blackPlayerId");
        var legacy = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("X-Player-Id", whiteId).GET().build();
        assertEquals(401, client.send(legacy, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertProblem(401, "UNAUTHORIZED", request("GET", path, blackId, null));
        assertProblem(403, "FORBIDDEN", request("GET", path, issue(), null));
        assertEquals(200, request("POST", path + "/moves", owner, move(UUID.randomUUID().toString(), 0, 4, 1)).statusCode());
        var spoof = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path + "/moves"))
                .header("Authorization", "Bearer " + owner).header("X-Player-Id", blackId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(move(UUID.randomUUID().toString(), 1, 4, 1))).build();
        var response = client.send(spoof, HttpResponse.BodyHandlers.ofString());
        assertProblem(409, "WRONG_TURN", response);
    }

    @Test void invitationIsSingleUseAndOwnerCannotClaimOtherSeat() throws Exception {
        var created = request("POST", "/games", owner, "{}");
        String path = created.headers().firstValue("Location").orElseThrow();
        String join = "{\"invitation\":\"" + value(created, "$.invitation") + "\"}";
        assertProblem(403, "FORBIDDEN", request("POST", path + "/join", owner, join));
        String guest = issue();
        assertEquals(200, request("POST", path + "/join", guest, join).statusCode());
        assertEquals(200, request("POST", path + "/join", guest, join).statusCode());
        assertProblem(409, "INVITATION_UNAVAILABLE", request("POST", path + "/join", issue(), join));
        assertProblem(409, "WRONG_TURN", request("POST", path + "/moves", guest, move(UUID.randomUUID().toString(), 0, 4, 1)));
    }

    @Test void conflictsValidationAndInvalidTokensDoNotMutateState() throws Exception {
        String path = request("POST", "/games", owner, "{}").headers().firstValue("Location").orElseThrow();
        String id = UUID.randomUUID().toString();
        assertProblem(409, "VERSION_CONFLICT", request("POST", path + "/moves", owner, move(id, 1, 4, 1)));
        assertProblem(422, "INVALID_MOVE", request("POST", path + "/moves", owner, move(id, 0, 2, 3)));
        assertEquals(0, (int) value(request("GET", path, owner, null), "$.version"));
        assertEquals(200, request("POST", path + "/moves", owner, move(id, 0, 4, 1)).statusCode());
        assertProblem(409, "COMMAND_CONFLICT", request("POST", path + "/moves", owner, move(id, 0, 4, 3)));
        assertProblem(401, "UNAUTHORIZED", request("GET", path, owner + "tampered", null));
        for (String body : new String[]{"{}", "null", "{", move(id, -1, 4, 1), move(id, 1, 8, 1)})
            assertProblem(400, "INVALID_REQUEST", request("POST", path + "/moves", owner, body));
    }
    private String issue() throws Exception { return value(request("POST", "/sessions", null, "{}"), "$.token"); }
    private String move(String id, long version, int row, int column) {
        return "{\"from\":{\"row\":5,\"column\":0},\"landings\":[{\"row\":" + row + ",\"column\":" + column
                + "}],\"expectedVersion\":" + version + ",\"clientMoveId\":\"" + id + "\"}";
    }
    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private <T> T value(HttpResponse<String> response, String path) { return JsonPath.read(response.body(), path); }
    private void assertProblem(int status, String code, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body()); assertEquals(code, value(response, "$.code"));
    }
}
