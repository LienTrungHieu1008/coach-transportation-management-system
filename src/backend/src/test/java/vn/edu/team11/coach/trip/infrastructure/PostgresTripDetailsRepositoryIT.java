package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import vn.edu.team11.coach.trip.application.TripDetailsModels.Request;
import vn.edu.team11.coach.trip.application.TripDetailsService;
import vn.edu.team11.coach.trip.application.TripSearchService;
import vn.edu.team11.coach.trip.application.TripSearchModels;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Result;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.StopFact;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.DATE;
import static vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Reason.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional(isolation = Isolation.REPEATABLE_READ)
@Sql("/fixtures/trip_demo.sql")
class PostgresTripDetailsRepositoryIT {
    @Autowired private TripDetailsService service;
    @Autowired private TripSearchService search;
    @Autowired private TripQueryPort p3;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @MockitoBean(name = "tripDetailsClock") private Clock clock;
    @MockitoBean(name = "tripSearchClock") private Clock searchClock;
    @BeforeEach
    void fixedClock() { when(clock.instant()).thenReturn(NOW); when(searchClock.instant()).thenReturn(NOW); }
    private Result details(UUID from, UUID to) { return service.details(new Request(DAY, from, to)); }

    @Test
    void q1P05BasicAndOptionalDescriptionUseActualDatabaseAndAgreeWithP2P3() throws Exception {
        var result = details(A, C);
        var item = search.search(new TripSearchModels.Request(A, C, DATE, 0, 20)).items().getFirst();
        var port = p3.getSeatAvailabilityFacts(DAY, A, C, NOW);
        assertThat(result.cutoff()).isEqualTo(item.bookingClosesAt()).isEqualTo(port.bookingClosesAt());
        assertThat(result.segment().fareAmountVnd()).isEqualTo(item.fareAmountVnd()).isEqualTo(250_000L);
        assertThat(result.segment().boardingTime()).isEqualTo(item.boardingTime());
        assertThat(result.segment().alightingTime()).isEqualTo(item.alightingTime());
        assertThat(result.stops()).extracting(StopFact::index).containsExactly(0, 1, 2);
        assertThat(result.fares()).extracting(f -> f.fareAmountVnd()).containsExactly(100_000L, 150_000L);
        assertThat(result.reasons()).isEmpty();
        mvc.perform(get("/api/v1/trips/" + DAY).param("fromStopId", A.toString()).param("toStopId", C.toString())
                        .with(user("passenger")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.selectedSegment.fareAmountVnd").value(250000))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.serverTime").value(NOW.toString()));
        var noPair = details(null, null);
        assertThat(noPair.segment()).isNull();
        assertThat(noPair.reasons()).isEmpty();
    }
    @Test
    void q1NotFoundAndKnownInvalidSegmentHaveDifferentStatuses() {
        code(() -> service.details(new Request(UUID.randomUUID(), A, C)), 404, "TRIP_NOT_FOUND");
        code(() -> details(C, A), 422, "INVALID_SEGMENT");
        code(() -> details(A, A), 422, "INVALID_SEGMENT");
        code(() -> details(UUID.randomUUID(), C), 422, "STOP_NOT_SERVED");
        UUID outside = UUID.randomUUID();
        jdbc.update("INSERT INTO stop(stop_id, stop_name, address) VALUES (?, 'Outside', 'Test')", outside);
        code(() -> details(outside, C), 422, "INVALID_SEGMENT");
    }
    @ParameterizedTest
    @EnumSource(value = TripStatus.class, names = {"Cancelled", "Departed", "Completed"})
    void q1NonscheduledTripStillReturnsFacts(TripStatus status) {
        jdbc.update("UPDATE trip SET status = ? WHERE trip_id = ?", status.name(), DAY);
        var result = details(A, C);
        assertThat(result.facts().status()).isEqualTo(status);
        assertThat(result.reasons()).containsExactly(TRIP_NOT_SCHEDULED);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(250_000L);
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void q1CutoffUsesFirstStopForEverySegmentAndMatchesP3(long millis) {
        when(clock.instant()).thenReturn(CUTOFF.plusMillis(millis));
        var result = details(B, C);
        assertThat(result.cutoff()).isEqualTo(CUTOFF);
        assertThat(result.reasons()).isEqualTo(millis < 0 ? java.util.List.of() : java.util.List.of(BOOKING_CLOSED));
        if (millis < 0) assertThat(p3.getSeatAvailabilityFacts(DAY, B, C, CUTOFF.plusMillis(millis)).bookingClosesAt()).isEqualTo(result.cutoff());
        else assertThatThrownBy(() -> p3.getSeatAvailabilityFacts(DAY, B, C, CUTOFF.plusMillis(millis)))
                .isInstanceOf(vn.edu.team11.coach.trip.contract.TripQueryException.class);
    }
    @Test
    void q1MultipleReasonsAccumulateEvenWhenTripIsClosed() {
        jdbc.update("UPDATE trip SET status = 'Cancelled' WHERE trip_id = ?", DAY);
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ? AND from_stop_id = ?", DAY, B);
        when(clock.instant()).thenReturn(CUTOFF);
        var result = details(A, B);
        assertThat(result.reasons()).containsExactly(TRIP_NOT_SCHEDULED, BOOKING_CLOSED, ESSENTIAL_DATA_INCOMPLETE);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(100_000L);
    }
    @Test
    void q1MissingFareOutsideSegmentPreservesPriceAndMissingInsideDoesNot() {
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ? AND from_stop_id = ?", DAY, B);
        var ab = details(A, B);
        assertThat(ab.segment().fareAmountVnd()).isEqualTo(100_000L);
        assertThat(ab.fares()).hasSize(1);
        assertThat(ab.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        assertThat(details(A, C).segment().fareAmountVnd()).isNull();
    }
    @Test
    void missingTripStopDoesNotRemoveMembershipOrInventStopRecord() {
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ?", DAY);
        jdbc.update("DELETE FROM trip_stop WHERE trip_id = ? AND stop_id = ?", DAY, B);
        var result = details(B, C);
        assertThat(result.segment()).isNotNull();
        assertThat(result.segment().boardingTime()).isNull();
        assertThat(result.stops()).extracting(StopFact::id).containsExactly(A, C);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void v3PreventsDeletingTripStopWhileReferencedLegFaresRemain() {
        // Preserve the real FK constraints. A savepoint allows checking the rejection
        // without leaving the test transaction aborted or deleting the fares first.
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try {
                assertThatThrownBy(() -> jdbc.update(
                        "DELETE FROM trip_stop WHERE trip_id = ? AND stop_id = ?", DAY, B))
                        .isInstanceOfSatisfying(DataIntegrityViolationException.class, failure -> {
                            assertThat(failure.getMostSpecificCause()).isInstanceOfSatisfying(SQLException.class, sql -> {
                                assertThat(sql.getSQLState()).isEqualTo("23503");
                                assertThat(sql.getMessage()).containsAnyOf("leg_fare_from_trip_stop_fk", "leg_fare_to_trip_stop_fk");
                            });
                        });
            } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
            }
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM leg_fare WHERE trip_id = ?", Integer.class, DAY)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip_stop WHERE trip_id = ? AND stop_id = ?", Integer.class, DAY, B)).isEqualTo(1);
        assertThat(details(A, C).segment().fareAmountVnd()).isEqualTo(250_000L);
        assertThat(details(A, C).reasons()).isEmpty();
    }
    @Test
    void missingCutoffDoesNotUseScheduleOrAnotherTimestamp() {
        jdbc.update("UPDATE trip_stop SET planned_arrival_time = planned_departure_time, planned_departure_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, A);
        when(clock.instant()).thenReturn(CUTOFF.plusSeconds(86400));
        var result = details(A, C);
        assertThat(result.cutoff()).isNull();
        assertThat(result.segment().boardingTime()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingIntermediateTimeDoesNotFallback() {
        jdbc.update("UPDATE trip_stop SET planned_departure_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, B);
        assertThat(details(B, C).segment().boardingTime()).isNull();
        assertThat(details(B, C).reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        jdbc.update("UPDATE trip_stop SET planned_departure_time = planned_arrival_time, planned_arrival_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, B);
        assertThat(details(A, B).segment().alightingTime()).isNull();
    }
    @Test
    void missingRouteStopConfigurationReturnsEmptyPartialDetailsNotInvalidSegment() {
        jdbc.update("DELETE FROM leg_fare");
        jdbc.update("DELETE FROM trip_stop");
        jdbc.update("DELETE FROM route_stop WHERE route_id = ?", ROUTE);
        var result = details(A, C);
        assertThat(result.segment()).isNull();
        assertThat(result.stops()).isEmpty();
        assertThat(result.cutoff()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @ParameterizedTest
    @ValueSource(longs = {0, 9007199254740991L, 9007199254740992L, Long.MAX_VALUE})
    void zeroAndSafeIntegerBoundariesReadFromPostgres(long amount) {
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = ? WHERE trip_id = ? AND from_stop_id = ?", amount, DAY, B);
        var result = details(B, C);
        if (amount <= TripSearchPolicy.MAX_SAFE_INTEGER) {
            assertThat(result.segment().fareAmountVnd()).isEqualTo(amount);
            assertThat(result.reasons()).isEmpty();
        } else {
            assertThat(result.segment().fareAmountVnd()).isNull();
            assertThat(result.fares()).hasSize(1);
            assertThat(details(A, B).segment().fareAmountVnd()).isEqualTo(100_000L);
            assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        }
    }
    @Test
    void segmentOverflowIsPartialAndDoesNotOverflowJavaOrRequireWholeTripSum() {
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = ? WHERE trip_id = ?", TripSearchPolicy.MAX_SAFE_INTEGER, DAY);
        assertThat(details(A, B).reasons()).isEmpty();
        assertThat(details(A, B).segment().fareAmountVnd()).isEqualTo(TripSearchPolicy.MAX_SAFE_INTEGER);
        assertThat(details(A, C).segment().fareAmountVnd()).isNull();
        assertThat(details(A, C).reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @ParameterizedTest
    @ValueSource(strings = {"route", "stop"})
    void inactiveRouteOrStopKeepsFactsButBlocksEligibility(String table) {
        if (table.equals("route")) jdbc.update("UPDATE route SET is_active = FALSE WHERE route_id = ?", ROUTE);
        else jdbc.update("UPDATE stop SET is_active = FALSE WHERE stop_id = ?", B);
        var result = details(A, C);
        assertThat(result.stops()).hasSize(3);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(250_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void q1MissingLayoutDoesNotEraseVerifiedSegmentPrice() {
        jdbc.update("DELETE FROM physical_seat WHERE seat_code = 'C2'");
        var result = details(A, C);
        assertThat(result.facts().vehicle()).isNotNull();
        assertThat(result.segment().fareAmountVnd()).isEqualTo(250_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void q1DatabaseFailureIs503ThroughActualServiceAndController() throws Exception {
        jdbc.execute("SET LOCAL search_path TO pg_catalog");
        mvc.perform(get("/api/v1/trips/" + DAY).with(user("passenger")))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("TRIP_DATA_UNAVAILABLE"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
    private void code(Runnable operation, int status, String code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(TripDetailsException.class, failure -> {
            assertThat(failure.status()).isEqualTo(status);
            assertThat(failure.code()).isEqualTo(code);
        });
    }
}
