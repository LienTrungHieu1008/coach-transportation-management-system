package vn.edu.team11.coach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIT {
    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void freshPostgresSchemaIsMigratedAndHealthRevealsOnlyStatus() throws Exception {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 16.");
        assertThat(jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null", String.class))
                .contains("1");
        var response = get("/api/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body())).isEqualTo(json.readTree("{\"status\":\"UP\"}"));
    }

    @Test
    void csrfBootstrapCreatesPersistedHttpOnlyLaxSession() throws Exception {
        var response = get("/api/session/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode token = json.readTree(response.body());
        assertThat(token.path("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        assertThat(token.path("token").asText()).isNotBlank();
        String sessionCookie = response.headers().firstValue("set-cookie").orElseThrow();
        assertThat(sessionCookie)
                .contains("SESSION=", "HttpOnly", "SameSite=Lax");
        assertThat(jdbc.queryForObject("select count(*) from spring_session", Long.class)).isPositive();
        assertThat(jdbc.queryForObject("select count(*) from spring_session_attributes", Long.class)).isPositive();
        assertThat(jdbc.queryForList("select max_inactive_interval from spring_session", Integer.class))
                .allMatch(timeout -> timeout == 1800);

        // Reuse the real cookie/token pair: CSRF passes, authentication still blocks the write.
        var write = HttpRequest.newBuilder(uri("/api/private/not-implemented"))
                .header("Cookie", sessionCookie.split(";", 2)[0])
                .header(token.path("headerName").asText(), token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(client.send(write, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test
    void unknownPrivateApiIsUnauthorizedAndWriteWithoutCsrfIsForbidden() throws Exception {
        assertThat(get("/api/private/not-implemented").statusCode()).isEqualTo(401);
        var request = HttpRequest.newBuilder(uri("/api/private/not-implemented"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
