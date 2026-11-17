package vn.edu.team11.coach.trip.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicStopsSecurityIT {
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    // No cookie store or authentication: each request is anonymous unless a CSRF session is supplied.
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void anonymousGetReadsRealPostgresThroughHttpSecurityFilter() throws Exception {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 16.");
        UUID stop = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        String name = "Public stop " + stop;
        // Committed JDBC fixture is visible to the server's separate HTTP transaction.
        try {
            jdbc.update("INSERT INTO stop(stop_id, stop_name, address) VALUES (?, ?, 'Test')", stop, name);
            jdbc.update("INSERT INTO route(route_id, route_code, route_name) VALUES (?, ?, 'Test')",
                    route, route.toString());
            jdbc.update("INSERT INTO route_stop(route_id, stop_id, stop_order) VALUES (?, ?, 0)", route, stop);

            var response = get("/api/v1/stops");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            var body = json.readTree(response.body());
            assertThat(body.path("items").findValuesAsText("id")).contains(stop.toString());
            assertThat(body.path("items").findValuesAsText("name")).contains(name);
            assertThat(body.path("serverTime").asText()).isNotBlank();

            jdbc.update("UPDATE stop SET is_active = FALSE WHERE stop_id = ?", stop);
            var afterUpdate = get("/api/v1/stops");
            assertThat(afterUpdate.statusCode()).isEqualTo(200);
            assertThat(json.readTree(afterUpdate.body()).path("items").findValuesAsText("id"))
                    .doesNotContain(stop.toString());
        } finally {
            jdbc.update("DELETE FROM route_stop WHERE route_id = ?", route);
            jdbc.update("DELETE FROM route WHERE route_id = ?", route);
            jdbc.update("DELETE FROM stop WHERE stop_id = ?", stop);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousWritesRequireRealCsrfSessionAndStillRequireAuthentication(String method) throws Exception {
        assertSecurityError(sendWrite(method, null, null), 403, "ACCESS_DENIED");

        var bootstrap = get("/api/session/csrf");
        assertThat(bootstrap.statusCode()).isEqualTo(200);
        var token = json.readTree(bootstrap.body());
        assertThat(token.path("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        assertThat(token.path("token").asText()).isNotBlank();
        String cookie = bootstrap.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];

        assertSecurityError(sendWrite(method, cookie, "invalid-token"), 403, "ACCESS_DENIED");
        assertSecurityError(sendWrite(method, cookie, token.path("token").asText()), 401, "UNAUTHENTICATED");
    }

    private void assertSecurityError(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
    }

    private HttpResponse<String> sendWrite(String method, String cookie, String token) throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/stops"))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (token != null) {
            request.header("X-CSRF-TOKEN", token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
