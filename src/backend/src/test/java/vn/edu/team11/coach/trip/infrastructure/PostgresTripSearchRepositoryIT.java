package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.application.TripSearchException;
import vn.edu.team11.coach.trip.application.TripSearchModels.Page;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;
import vn.edu.team11.coach.trip.application.TripSearchService;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional(isolation = Isolation.REPEATABLE_READ)
@Sql("/fixtures/trip_demo.sql")
class PostgresTripSearchRepositoryIT {
    @Autowired private TripSearchService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TripQueryPort p3;
    @Autowired private MockMvc mvc;
    @MockitoBean(name = "tripSearchClock") private Clock clock;

    @BeforeEach
    void fixedClock() { when(clock.instant()).thenReturn(NOW); }

    private Page search(UUID from, UUID to, java.time.LocalDate date, int page, int size) {
        return service.search(new Request(from, to, date, page, size));
    }

    @Test
    void q1BasicFlowAndSegmentFareUseActualPostgresRecords() {
        var result = search(A, C, DATE, 0, 20);
        assertThat(result.items()).extracting(item -> item.tripId()).containsExactly(DAY, NIGHT);
        assertThat(result.items()).extracting(item -> item.fareAmountVnd()).containsExactly(250_000L, 250_000L);
        assertThat(result.items().getFirst().bookingClosesAt()).isEqualTo(CUTOFF);
        assertThat(result.items().getFirst().route().name()).isEqualTo("Demo Route A-B-C");
        assertThat(result.serverTime()).isEqualTo(NOW);
    }

    @Test
    void q1OvernightBoardingUsesVietnamDateNotUtcOrTripDepartureDate() {
        var nextDay = search(B, C, DATE.plusDays(1), 0, 20);
        assertThat(nextDay.items()).extracting(item -> item.tripId()).containsExactly(NIGHT);
        assertThat(nextDay.items().getFirst().boardingTime()).isEqualTo(Instant.parse("2026-11-20T17:30:00Z"));
        assertThat(nextDay.items().getFirst().fareAmountVnd()).isEqualTo(150_000);
        assertThat(search(B, C, DATE, 0, 20).items()).extracting(item -> item.tripId()).containsExactly(DAY);
    }

    @Test
    void overnightIntermediateBoardingStillClosesAtFirstStopCutoff() {
        Instant cutoff = Instant.parse("2026-11-20T15:00:00Z");
        when(clock.instant()).thenReturn(cutoff.minusMillis(1));
        assertThat(search(B, C, DATE.plusDays(1), 0, 20).items()).hasSize(1);
        when(clock.instant()).thenReturn(cutoff);
        assertThat(search(B, C, DATE.plusDays(1), 0, 20).items()).isEmpty();
        when(clock.instant()).thenReturn(cutoff.plusMillis(1));
        assertThat(search(B, C, DATE.plusDays(1), 0, 20).items()).isEmpty();
    }

    @Test
    void cutoffAndCompleteFareCoverageAgreeWithExistingP3Rules() {
        assertThat(p3.getSeatAvailabilityFacts(DAY, A, B, CUTOFF.minusMillis(1)).bookingClosesAt()).isEqualTo(CUTOFF);
        when(clock.instant()).thenReturn(CUTOFF);
        assertThat(search(A, B, DATE, 0, 20).items()).extracting(item -> item.tripId()).containsExactly(NIGHT);
        assertThatThrownBy(() -> p3.getSeatAvailabilityFacts(DAY, A, B, CUTOFF))
                .isInstanceOfSatisfying(TripQueryException.class,
                        exception -> assertThat(exception.getCode().name()).isEqualTo("TRIP_NOT_BOOKABLE"));
        when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ? AND from_stop_id = ?", DAY, B);
        unavailable(() -> search(A, B, DATE, 0, 20));
        assertThatThrownBy(() -> p3.getSeatAvailabilityFacts(DAY, A, B, NOW))
                .isInstanceOfSatisfying(TripQueryException.class,
                        exception -> assertThat(exception.getCode().name()).isEqualTo("ESSENTIAL_DATA_INCOMPLETE"));
    }

    @Test
    void zeroAndSafeFareBoundariesDoNotRequireSummingWholeTrip() {
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = 0");
        assertThat(search(A, C, DATE, 0, 20).items()).allSatisfy(item -> assertThat(item.fareAmountVnd()).isZero());
        assertThat(p3.getSeatAvailabilityFacts(DAY, A, B, NOW).tripId()).isEqualTo(DAY);
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = ?", TripSearchPolicy.MAX_SAFE_INTEGER);
        assertThat(search(A, B, DATE, 0, 20).items()).allSatisfy(
                item -> assertThat(item.fareAmountVnd()).isEqualTo(TripSearchPolicy.MAX_SAFE_INTEGER));
        unavailable(() -> search(A, C, DATE, 0, 20));
    }

    @Test
    void oversizedLegOutsideSegmentReturns503() {
        jdbc.update("UPDATE leg_fare SET fare_amount_vnd = ? WHERE trip_id = ? AND from_stop_id = ?",
                TripSearchPolicy.MAX_SAFE_INTEGER + 1, DAY, B);
        unavailable(() -> search(A, B, DATE, 0, 20));
    }

    @Test
    void candidateOutsideRequestedPageStillMustBeComplete() {
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ? AND from_stop_id = ?", NIGHT, B);
        unavailable(() -> search(A, B, DATE, 0, 1));
        unavailable(() -> search(A, B, DATE, Integer.MAX_VALUE, 100));
    }

    @Test
    void missingBoardingDepartureDoesNotFallbackToArrivalOrDisappear() {
        jdbc.update("UPDATE trip_stop SET planned_departure_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, B);
        unavailable(() -> search(B, C, DATE, 0, 20));
        when(clock.instant()).thenReturn(CUTOFF);
        // The same damaged Trip is provably closed, so it must now be excluded.
        assertThat(search(B, C, DATE, 0, 20).items()).isEmpty();
    }

    @Test
    void missingArrivalAndMissingCutoffProduce503ForPotentialMatches() {
        jdbc.update("UPDATE trip_stop SET planned_arrival_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, B);
        unavailable(() -> search(A, B, DATE, 0, 20));
        jdbc.update("UPDATE trip_stop SET planned_arrival_time = planned_departure_time WHERE trip_id = ? AND stop_id = ?", DAY, B);
        jdbc.update("UPDATE trip_stop SET planned_arrival_time = planned_departure_time, planned_departure_time = NULL WHERE trip_id = ? AND stop_id = ?", DAY, A);
        unavailable(() -> search(B, C, DATE, 0, 20));
        assertThat(search(B, C, DATE.plusDays(5), 0, 20).items()).isEmpty();
    }

    @Test
    void missingTripStopOrLayoutIsNotSilentlyRemovedByInnerJoin() {
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ?", DAY);
        jdbc.update("DELETE FROM trip_stop WHERE trip_id = ? AND stop_id = ?", DAY, B);
        unavailable(() -> search(A, C, DATE, 0, 20));
    }

    @Test
    void incompleteLayoutFailsOnlyMatchingCandidates() {
        jdbc.update("DELETE FROM physical_seat WHERE seat_code = 'C2'");
        unavailable(() -> search(A, B, DATE, 0, 20));
        assertThat(search(A, B, DATE.plusDays(5), 0, 20).items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TripStatus.class, names = {"Cancelled", "Departed", "Completed"})
    void nonscheduledDamagedTripDoesNotPoisonSearch(TripStatus status) {
        jdbc.update("UPDATE trip SET status = ? WHERE trip_id = ?", status.name(), DAY);
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ?", DAY);
        assertThat(search(A, B, DATE, 0, 20).items()).extracting(item -> item.tripId()).containsExactly(NIGHT);
    }

    @Test
    void knownWrongDateOrClosedDamagedTripDoesNotPoisonSearch() {
        jdbc.update("DELETE FROM leg_fare WHERE trip_id = ?", DAY);
        assertThat(search(A, B, DATE.plusDays(5), 0, 20).items()).isEmpty();
        when(clock.instant()).thenReturn(CUTOFF);
        assertThat(search(A, B, DATE, 0, 20).items()).extracting(item -> item.tripId()).containsExactly(NIGHT);
    }

    @Test
    void servedStopAndOrderedPairValidationAreIndependentOfTrips() {
        assertCode(() -> search(C, A, DATE, 0, 20), "INVALID_SEGMENT");
        assertCode(() -> search(A, A, DATE, 0, 20), "INVALID_SEGMENT");
        assertCode(() -> search(UUID.randomUUID(), B, DATE, 0, 20), "STOP_NOT_SERVED");
        jdbc.update("UPDATE stop SET is_active = FALSE WHERE stop_id = ?", A);
        assertCode(() -> search(A, B, DATE, 0, 20), "STOP_NOT_SERVED");
    }

    @Test
    void activeOrphanAndStopsOnlyOnInactiveRouteAreUnserved() {
        UUID orphan = UUID.randomUUID();
        jdbc.update("INSERT INTO stop(stop_id, stop_name, address) VALUES (?, 'Orphan', 'Test')", orphan);
        assertCode(() -> search(orphan, B, DATE, 0, 20), "STOP_NOT_SERVED");
        jdbc.update("UPDATE route SET is_active = FALSE WHERE route_id = ?", ROUTE);
        assertCode(() -> search(A, B, DATE, 0, 20), "STOP_NOT_SERVED");
    }

    @Test
    void paginationCountsAndUuidTieBreakRemainStable() {
        UUID high = UUID.fromString("f0000000-0000-4000-8000-000000000001");
        UUID low = UUID.fromString("00000000-0000-4000-8000-000000000001");
        copyTrip(high);
        copyTrip(low);
        var first = search(A, B, DATE, 0, 2);
        var second = search(A, B, DATE, 1, 2);
        assertThat(first.items()).extracting(item -> item.tripId()).containsExactly(low, DAY);
        assertThat(second.items()).extracting(item -> item.tripId()).containsExactly(high, NIGHT);
        assertThat(first.totalElements()).isEqualTo(4);
        assertThat(first.totalPages()).isEqualTo(2);
        var beyond = search(A, B, DATE, Integer.MAX_VALUE, 100);
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.totalElements()).isEqualTo(4);
        assertThat(beyond.totalPages()).isEqualTo(1);
    }

    @Test
    void emptyResultAndPastVietnamDateHaveDifferentOutcomes() {
        var empty = search(A, B, DATE.plusDays(5), 0, 20);
        assertThat(empty.items()).isEmpty();
        assertThat(empty.totalElements()).isZero();
        assertThat(empty.totalPages()).isZero();
        when(clock.instant()).thenReturn(Instant.parse("2026-11-20T17:00:00Z"));
        assertCode(() -> search(A, B, DATE, 0, 20), "PAST_BOARDING_DATE");
    }

    @Test
    void realDatabaseAccessFailureIs503() {
        jdbc.execute("SET LOCAL search_path TO pg_catalog");
        unavailable(() -> search(A, B, DATE, 0, 20));
    }

    @Test
    void authenticatedHttpContractRunsThroughRealServiceAndDatabase() throws Exception {
        mvc.perform(get("/api/v1/trips").param("fromStopId", A.toString()).param("toStopId", C.toString())
                        .param("boardingDate", DATE.toString()).with(user("passenger")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].fareAmountVnd").value(250000))
                .andExpect(jsonPath("$.serverTime").value(NOW.toString()));
    }

    private void copyTrip(UUID id) {
        jdbc.update("INSERT INTO trip SELECT ?, route_id, schedule_id, vehicle_id, departure_date, status FROM trip WHERE trip_id = ?", id, DAY);
        jdbc.update("INSERT INTO trip_stop SELECT ?, route_id, stop_id, planned_arrival_time, planned_departure_time FROM trip_stop WHERE trip_id = ?", id, DAY);
        jdbc.update("INSERT INTO leg_fare SELECT ?, from_stop_id, to_stop_id, fare_amount_vnd FROM leg_fare WHERE trip_id = ?", id, DAY);
    }

    private void unavailable(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(TripSearchException.class, exception -> {
            assertThat(exception.status()).isEqualTo(503);
            assertThat(exception.code()).isEqualTo("TRIP_DATA_UNAVAILABLE");
        });
    }

    private void assertCode(Runnable operation, String code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(TripSearchException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
