package br.com.highonline.checkers.game.infrastructure.postgres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PG_TEST_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "checkers.database.url=${PG_TEST_URL}", "checkers.database.username=${PG_TEST_USER}",
        "checkers.database.password=${PG_TEST_PASSWORD}"})
@ActiveProfiles("local-postgres")
class PostgresApiTest {
    @LocalServerPort int port;
    @Autowired DataSource dataSource;

    @Test
    void profileWiresApiToPersistentAdapter() throws Exception {
        UUID black = UUID.randomUUID(), white = UUID.randomUUID();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/games"))
                .header("Content-Type", "application/json").header("X-Player-Id", white.toString())
                .POST(HttpRequest.BodyPublishers.ofString("{\"blackPlayerId\":\"" + black
                        + "\",\"whitePlayerId\":\"" + white + "\"}")).build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        String location = response.headers().firstValue("Location").orElseThrow();
        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        assertEquals(0L, new JdbcTemplate(dataSource).queryForObject("SELECT version FROM games WHERE id = ?", Long.class, id));
    }
}
