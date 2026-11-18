package vn.edu.team11.coach.trip.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.UUID;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.AopTestUtils;
import vn.edu.team11.coach.trip.infrastructure.PostgresTripDetailsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/fixtures/trip_demo.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "/fixtures/trip_snapshot_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class PublicTripDetailsSecurityIT {
    private static final String PATH = "/api/v1/trips/" + DAY;
    private static final String PAIR = "?fromStopId=" + A + "&toStopId=" + C;
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @MockitoBean(name = "tripDetailsClock") private Clock clock;
    @MockitoBean(name = "tripSearchClock") private Clock searchClock;
    @MockitoSpyBean private PostgresTripDetailsRepository repository;
    // No authentication, cookie store, test user or filter overrides.
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void fixedClocks() { when(clock.instant()).thenReturn(NOW); when(searchClock.instant()).thenReturn(NOW); }

    @Test
    void anonymousDetailsReturnsExactJsonFromRealPostgresAndReflectsCommittedUpdates() throws Exception {
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL 16.");
        var response = get(PATH + PAIR);
        assertPublicResponse(response, 200);
        assertThat(json.readTree(response.body())).isEqualTo(json.readTree("""
                {"tripId":"11111111-1111-4111-8111-111111111111","status":"Scheduled",
                 "route":{"id":"44444444-4444-4444-8444-444444444444","name":"Demo Route A-B-C"},
                 "vehicle":{"id":"66666666-6666-4666-8666-666666666666","vehicleType":"Demo coach",
                            "seatLayoutId":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"},
                 "stops":[
                   {"stop":{"id":"22222222-2222-4222-8222-222222222222","name":"Demo Stop A"},"index":0,
                    "plannedArrivalTime":null,"plannedDepartureTime":"2026-11-20T02:00:00Z"},
                   {"stop":{"id":"33333333-3333-4333-8333-333333333333","name":"Demo Stop B"},"index":1,
                    "plannedArrivalTime":"2026-11-20T04:00:00Z","plannedDepartureTime":"2026-11-20T04:00:00Z"},
                   {"stop":{"id":"99999999-9999-4999-8999-999999999999","name":"Demo Stop C"},"index":2,
                    "plannedArrivalTime":"2026-11-20T06:00:00Z","plannedDepartureTime":null}],
                 "legFares":[
                   {"fromStopId":"22222222-2222-4222-8222-222222222222","toStopId":"33333333-3333-4333-8333-333333333333","fareAmountVnd":100000},
                   {"fromStopId":"33333333-3333-4333-8333-333333333333","toStopId":"99999999-9999-4999-8999-999999999999","fareAmountVnd":150000}],
                 "description":null,"selectedSegment":{
                   "fromStopId":"22222222-2222-4222-8222-222222222222","toStopId":"99999999-9999-4999-8999-999999999999",
                   "boardingTime":"2026-11-20T02:00:00Z","alightingTime":"2026-11-20T06:00:00Z","fareAmountVnd":250000},
                 "bookingClosesAt":"2026-11-20T01:30:00Z","bookingEligibility":{"allowed":true,"reasons":[]},
                 "serverTime":"2026-11-20T00:00:00Z"}
                """));
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = 120000 WHERE trip_id = ? AND from_stop_id = ?", DAY, A);
        var updated = get(PATH + PAIR);
        assertPublicResponse(updated, 200);
        assertThat(json.readTree(updated.body()).path("selectedSegment").path("fareAmountVnd").asLong()).isEqualTo(270_000);
    }
    @Test
    void anonymousDetailsWithoutSegmentIs200WithExplicitNull() throws Exception {
        var response = get(PATH);
        assertPublicResponse(response, 200);
        var body = json.readTree(response.body());
        assertThat(body.size()).isEqualTo(11);
        assertThat(body.get("selectedSegment").isNull()).isTrue();
        assertThat(body.path("bookingEligibility").path("allowed").asBoolean()).isTrue();
    }
    @ParameterizedTest
    @ValueSource(strings = {"bad-path", "bad-query", "unpaired-from", "unpaired-to", "duplicate-from", "duplicate-to"})
    void anonymousInputErrorsReachParserAs400(String input) throws Exception {
        String path = switch (input) {
            case "bad-path" -> "/api/v1/trips/abc";
            case "bad-query" -> PATH + "?fromStopId=abc&toStopId=" + C;
            case "unpaired-from" -> PATH + "?fromStopId=" + A;
            case "unpaired-to" -> PATH + "?toStopId=" + C;
            case "duplicate-from" -> PATH + PAIR + "&fromStopId=" + A;
            default -> PATH + PAIR + "&toStopId=" + C;
        };
        assertPublicError(get(path), 400, "INVALID_REQUEST");
        verify(spyTarget(), never()).load(any());
    }
    @Test
    void anonymousMissingTripIs404AndInvalidSegmentIs422() throws Exception {
        assertPublicError(get("/api/v1/trips/" + UUID.randomUUID()), 404, "TRIP_NOT_FOUND");
        assertPublicError(get(PATH + "?fromStopId=" + C + "&toStopId=" + A), 422, "INVALID_SEGMENT");
    }
    @Test
    void anonymousActualSqlFailureIs503AndDoesNotLeakInternals() throws Exception {
        var target = spyTarget();
        // Inject only a transaction-local PostgreSQL failure on the real server thread.
        // The production repository still executes its actual SQL; no mocked result/error.
        doAnswer(invocation -> {
            jdbc.execute("SET LOCAL search_path TO pg_catalog");
            return invocation.callRealMethod();
        }).when(target).load(DAY);
        try {
            var response = get(PATH + PAIR);
            assertPublicError(response, 503, "TRIP_DATA_UNAVAILABLE");
            assertThat(json.readTree(response.body())).isEqualTo(json.readTree("""
                    {"code":"TRIP_DATA_UNAVAILABLE","message":"Trip information could not be verified. Please retry.","fieldErrors":[]}
                    """));
        } finally {
            reset(target);
        }
        // SET LOCAL has ended with the failed request transaction, not poisoned the pool.
        assertPublicResponse(get(PATH + PAIR), 200);
    }
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousWritesRequireRealCsrfSessionAndStillRequireAuthentication(String method) throws Exception {
        assertSecurityError(write(method, null, null), 403, "ACCESS_DENIED");
        var bootstrap = get("/api/session/csrf");
        assertThat(bootstrap.statusCode()).isEqualTo(200);
        var token = json.readTree(bootstrap.body());
        assertThat(token.path("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        assertThat(token.path("token").asText()).isNotBlank();
        String cookie = bootstrap.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        assertSecurityError(write(method, cookie, "invalid-token"), 403, "ACCESS_DENIED");
        assertSecurityError(write(method, cookie, token.path("token").asText()), 401, "UNAUTHENTICATED");
        verify(spyTarget(), never()).load(any());
    }
    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/trips/", "/api/v1/trips/11111111-1111-4111-8111-111111111111/",
            "/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability/private",
            "/api/v1/trips/11111111-1111-4111-8111-111111111111/operations",
            "/api/v1/trips/11111111-1111-4111-8111-111111111111/nested/deeper",
            "/api/private/p4-security-probe", "/api/v1/stops/other", "/api/v1/other"})
    void excludedRoutesAreRejectedBySecurityNotByMissingMvcMapping(String path) throws Exception {
        assertSecurityError(get(path), 401, "UNAUTHENTICATED");
        verify(spyTarget(), never()).load(any());
    }
    @Test
    void p2AnonymousCatalogueAndSearchStillReturnRealData() throws Exception {
        var stops = get("/api/v1/stops");
        assertPublicResponse(stops, 200);
        assertThat(json.readTree(stops.body()).path("items").findValuesAsText("id")).contains(A.toString(), B.toString(), C.toString());
        var search = get("/api/v1/trips" + PAIR + "&boardingDate=2026-11-20");
        assertPublicResponse(search, 200);
        assertThat(json.readTree(search.body()).path("items").findValuesAsText("tripId")).containsExactly(DAY.toString(), NIGHT.toString());
    }
    private PostgresTripDetailsRepository spyTarget() { return AopTestUtils.getUltimateTargetObject(repository); }
    private void assertPublicResponse(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
    }
    private void assertPublicError(HttpResponse<String> response, int status, String code) throws Exception {
        assertPublicResponse(response, status);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
    }
    private void assertSecurityError(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo(code);
    }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> write(String method, String cookie, String token) throws Exception {
        var request = HttpRequest.newBuilder(uri(PATH)).method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) request.header("Cookie", cookie);
        if (token != null) request.header("X-CSRF-TOKEN", token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
}
