package br.com.highonline.checkers.game.infrastructure.http;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
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
    private final String black = UUID.randomUUID().toString(), white = UUID.randomUUID().toString();

    @Test
    void createReadMoveAndReplay() throws Exception {
        var created = create();
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        assertEquals("WHITE", value(created, "$.turn"));
        assertEquals(24, (int) value(created, "$.pieces.length()"));
        assertEquals(7, (int) value(created, "$.legalMoves.length()"));
        assertEquals(white, value(created, "$.currentPlayerId"));
        assertEquals(200, request("GET", location, black, null).statusCode());
        String body = move(UUID.randomUUID().toString(), 0, 4, 1);
        var accepted = request("POST", location + "/moves", white, body);
        assertEquals(200, accepted.statusCode());
        assertEquals(1, (int) value(accepted, "$.version"));
        assertEquals("BLACK", value(accepted, "$.turn"));
        var replay = request("POST", location + "/moves", white, body);
        assertEquals(200, replay.statusCode());
        assertEquals(accepted.body(), replay.body());
        assertEquals(1, (int) value(request("GET", location, white, null), "$.version"));
    }

    @Test
    void reportsConflictsAndDoesNotChangeStateOnInvalidMove() throws Exception {
        String location = create().headers().firstValue("Location").orElseThrow();
        String id = UUID.randomUUID().toString();
        assertProblem(409, "WRONG_TURN", request("POST", location + "/moves", black, move(id, 0, 4, 1)));
        assertProblem(409, "VERSION_CONFLICT", request("POST", location + "/moves", white, move(id, 1, 4, 1)));
        assertProblem(422, "INVALID_MOVE", request("POST", location + "/moves", white, move(id, 0, 2, 3)));
        assertEquals(0, (int) value(request("GET", location, white, null), "$.version"));
        assertEquals(200, request("POST", location + "/moves", white, move(id, 0, 4, 1)).statusCode());
        assertProblem(409, "COMMAND_CONFLICT", request("POST", location + "/moves", white, move(id, 0, 4, 3)));
    }

    @Test
    void rejectsNonParticipantsAndUnknownGames() throws Exception {
        String location = create().headers().firstValue("Location").orElseThrow();
        String outsider = UUID.randomUUID().toString();
        assertProblem(403, "FORBIDDEN", request("GET", location, outsider, null));
        assertProblem(403, "FORBIDDEN", request("POST", location + "/moves", outsider,
                move(UUID.randomUUID().toString(), 0, 4, 1)));
        assertProblem(403, "FORBIDDEN", request("POST", "/games", outsider, players()));
        assertProblem(404, "NOT_FOUND", request("GET", "/games/" + UUID.randomUUID(), white, null));
    }

    @Test
    void malformedRequestsHaveConsistentBadRequestResponse() throws Exception {
        assertProblem(400, "INVALID_REQUEST", request("POST", "/games", null, players()));
        assertProblem(400, "INVALID_REQUEST", request("GET", "/games/not-a-uuid", white, null));
        assertProblem(400, "INVALID_REQUEST", request("POST", "/games", "not-a-uuid", players()));
        assertProblem(400, "INVALID_REQUEST", request("POST", "/games", white, "{}"));
        assertProblem(400, "INVALID_REQUEST", request("POST", "/games", white, "{"));
        String location = create().headers().firstValue("Location").orElseThrow();
        for (String body : new String[]{"{}", "null",
                move(UUID.randomUUID().toString(), 0, 8, 1),
                move(UUID.randomUUID().toString(), -1, 4, 1),
                "{\"from\":{\"row\":5,\"column\":0},\"landings\":[null],\"expectedVersion\":0,\"clientMoveId\":\"" + UUID.randomUUID() + "\"}"}) {
            assertProblem(400, "INVALID_REQUEST", request("POST", location + "/moves", white, body));
        }
    }

    private HttpResponse<String> create() throws Exception { return request("POST", "/games", white, players()); }
    private String players() { return "{\"blackPlayerId\":\"" + black + "\",\"whitePlayerId\":\"" + white + "\"}"; }
    private String move(String id, long version, int row, int column) {
        return "{\"from\":{\"row\":5,\"column\":0},\"landings\":[{\"row\":" + row + ",\"column\":" + column
                + "}],\"expectedVersion\":" + version + ",\"clientMoveId\":\"" + id + "\"}";
    }
    private HttpResponse<String> request(String method, String path, String actor, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (actor != null) builder.header("X-Player-Id", actor);
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private <T> T value(HttpResponse<String> response, String path) { return JsonPath.read(response.body(), path); }
    private void assertProblem(int status, String code, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(code, value(response, "$.code"));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
    }
}
