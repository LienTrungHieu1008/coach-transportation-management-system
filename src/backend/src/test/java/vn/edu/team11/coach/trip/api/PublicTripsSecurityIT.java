package vn.edu.team11.coach.trip.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/fixtures/trip_demo.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "/fixtures/trip_snapshot_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class PublicTripsSecurityIT {
    private static final String SEARCH = "/api/v1/trips?fromStopId=" + A + "&toStopId=" + C
            + "&boardingDate=2026-11-20";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @MockitoBean(name = "tripSearchClock") private Clock clock;

    // No authentication, cookie store or test user; requests traverse the real HTTP filter chain.
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void fixedClock() { when(clock.instant()).thenReturn(NOW); }

    @Test
    void anonymousSearchReturnsExactFixtureJsonFromRealPostgres() throws Exception {
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL 16.");
        var response = get(SEARCH);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(json.readTree(response.body())).isEqualTo(json.readTree("""
                {"items":[
                  {"tripId":"11111111-1111-4111-8111-111111111111",
                   "route":{"id":"44444444-4444-4444-8444-444444444444","name":"Demo Route A-B-C"},
                   "fromStop":{"id":"22222222-2222-4222-8222-222222222222","name":"Demo Stop A"},
                   "toStop":{"id":"99999999-9999-4999-8999-999999999999","name":"Demo Stop C"},
                   "boardingTime":"2026-11-20T02:00:00Z","alightingTime":"2026-11-20T06:00:00Z",
                   "vehicleType":"Demo coach","fareAmountVnd":250000,"bookingClosesAt":"2026-11-20T01:30:00Z"},
                  {"tripId":"11111111-1111-4111-8111-111111111112",
                   "route":{"id":"44444444-4444-4444-8444-444444444444","name":"Demo Route A-B-C"},
                   "fromStop":{"id":"22222222-2222-4222-8222-222222222222","name":"Demo Stop A"},
                   "toStop":{"id":"99999999-9999-4999-8999-999999999999","name":"Demo Stop C"},
                   "boardingTime":"2026-11-20T15:30:00Z","alightingTime":"2026-11-20T19:30:00Z",
                   "vehicleType":"Demo coach","fareAmountVnd":250000,"bookingClosesAt":"2026-11-20T15:00:00Z"}],
                 "page":0,"size":20,"totalElements":2,"totalPages":1,"serverTime":"2026-11-20T00:00:00Z"}
                """));

        // A committed DB update must be reflected by the next anonymous HTTP request.
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = 120000 WHERE trip_id = ? AND from_stop_id = ?", DAY, A);
        var updated = get(SEARCH);
        assertThat(updated.statusCode()).isEqualTo(200);
        var items = json.readTree(updated.body()).path("items");
        assertThat(items.get(0).path("tripId").asText()).isEqualTo(DAY.toString());
        assertThat(items.get(0).path("fareAmountVnd").asLong()).isEqualTo(270000);
        assertThat(items.get(1).path("fareAmountVnd").asLong()).isEqualTo(250000);
    }

    @Test
    void anonymousSearchRetainsValidationAndDatabaseFailureMapping() throws Exception {
        assertSecurityError(get(SEARCH + "&size=20&size=20"), 400, "INVALID_REQUEST");
        when(clock.instant()).thenReturn(NOW.plusSeconds(86400));
        assertSecurityError(get(SEARCH), 422, "PAST_BOARDING_DATE");
        when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ? AND from_stop_id = ?", DAY, B);
        assertSecurityError(get(SEARCH), 503, "TRIP_DATA_UNAVAILABLE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousWritesStillRequireCsrfAndAuthentication(String method) throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/trips/", "/api/v1/trips/11111111-1111-4111-8111-111111111111/operations",
            "/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability"})
    void unapprovedTripPathsStillRequireAuthentication(String path) throws Exception {
        assertSecurityError(get(path), 401, "UNAUTHENTICATED");
    }

    private void assertSecurityError(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
        if (status != 401 && status != 403) {
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
    }

    private HttpResponse<String> sendWrite(String method, String cookie, String token) throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/trips"))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) request.header("Cookie", cookie);
        if (token != null) request.header("X-CSRF-TOKEN", token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
}
