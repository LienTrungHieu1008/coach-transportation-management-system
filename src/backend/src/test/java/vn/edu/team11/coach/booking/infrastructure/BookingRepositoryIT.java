package vn.edu.team11.coach.booking.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.booking.domain.BookingStatus;
import vn.edu.team11.coach.booking.domain.SeatAllocation;
import vn.edu.team11.coach.booking.domain.SeatAllocationService;
import vn.edu.team11.coach.booking.domain.Segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.team11.coach.booking.domain.SeatAllocation.Kind.Confirmed;
import static vn.edu.team11.coach.booking.domain.SeatAllocation.Kind.Hold;
import static vn.edu.team11.coach.booking.domain.SeatAllocationService.SeatStatus.FREE;
import static vn.edu.team11.coach.booking.domain.SeatAllocationService.SeatStatus.UNAVAILABLE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Sql({"/fixtures/identity_accounts.sql", "/fixtures/trip_demo.sql", "/fixtures/booking_availability.sql"})
class BookingRepositoryIT {
    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_TRIP = UUID.fromString("11111111-1111-4111-8111-111111111112");
    private static final UUID A1 = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final UUID A2 = UUID.fromString("88888888-8888-4888-8888-888888888888");
    private static final UUID B1 = UUID.fromString("77777777-7777-4777-8777-777777777778");
    private static final UUID B2 = UUID.fromString("77777777-7777-4777-8777-777777777779");
    private static final UUID C1 = UUID.fromString("77777777-7777-4777-8777-777777777780");
    private static final UUID C2 = UUID.fromString("77777777-7777-4777-8777-777777777781");
    private static final List<UUID> LAYOUT = List.of(A1, A2, B1, B2, C1, C2);
    private static final Segment AC = new Segment(0, 2);
    private static final Segment BC = new Segment(1, 2);
    private static final Instant AS_OF = Instant.parse("2026-11-19T00:08:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-11-19T00:10:00Z");
    private static final Instant OLD_EXPIRY = Instant.parse("2026-11-18T23:50:00Z");

    @Autowired private BookingRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void mapsParentStatusAndEveryAllocationFactIncludingNullableTimes() {
        assertThat(repository.findCandidates(TRIP, AC)).containsExactlyInAnyOrder(
                new SeatAllocation(TRIP, A2, new Segment(0, 1), BookingStatus.Confirmed, Confirmed, null, null),
                new SeatAllocation(TRIP, B1, AC, BookingStatus.Confirmed, Confirmed, OLD_EXPIRY, null),
                new SeatAllocation(TRIP, B2, BC, BookingStatus.Pending, Hold, EXPIRES, null),
                new SeatAllocation(TRIP, C1, AC, BookingStatus.Pending, Hold, OLD_EXPIRY, null));
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void readsCurrentParentStatusRatherThanInferringItFromKind(BookingStatus status) {
        jdbc.update("update booking set status = ? where booking_id = ?", status.name(),
                UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd03"));
        assertThat(repository.findCandidates(TRIP, BC)).filteredOn(a -> a.physicalSeatId().equals(B2))
                .singleElement().extracting(SeatAllocation::bookingStatus).isEqualTo(status);
    }

    @Test
    void excludesAdjacentSegmentsInBothDirectionsButIncludesIntersectingSegments() {
        assertThat(repository.findCandidates(TRIP, BC)).extracting(SeatAllocation::physicalSeatId)
                .containsExactlyInAnyOrder(B1, B2, C1);
        assertThat(repository.findCandidates(TRIP, new Segment(0, 1)))
                .extracting(SeatAllocation::physicalSeatId).containsExactlyInAnyOrder(A2, B1, C1);
    }

    @Test
    void excludesReleasedHistoryButMapsCancelledUnreleasedFactsForCoreEvaluation() {
        assertThat(repository.findCandidates(TRIP, AC)).extracting(SeatAllocation::physicalSeatId).doesNotContain(C2);
        jdbc.update("update seat_allocation set released_at = null where physical_seat_id = ?", C2);
        assertThat(repository.findCandidates(TRIP, AC)).filteredOn(a -> a.physicalSeatId().equals(C2))
                .containsExactly(new SeatAllocation(TRIP, C2, AC, BookingStatus.Cancelled, Hold, EXPIRES, null));
        assertThat(new SeatAllocationService().getAvailability(TRIP, AC, LAYOUT,
                repository.findCandidates(TRIP, AC), AS_OF)).filteredOn(s -> s.physicalSeatId().equals(C2))
                .containsExactly(state(C2, FREE, null));
    }

    @Test
    void excludesOtherTripsAndReturnsEmptyForNoCandidates() {
        assertThat(repository.findCandidates(OTHER_TRIP, AC)).isEmpty();
        assertThat(repository.findCandidates(UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"), AC)).isEmpty();
        assertThat(repository.findCandidates(TRIP, new Segment(2, 3))).isEmpty();
    }

    @Test
    void fixedSixStatesProduceExpectedAcAndBcResultsInVerifiedLayoutOrder() {
        List<UUID> layout = jdbc.queryForList("""
                select physical_seat_id from physical_seat where vehicle_id = ?
                order by deck, seat_row, seat_column
                """, UUID.class, UUID.fromString("66666666-6666-4666-8666-666666666666"));
        assertThat(layout).containsExactlyElementsOf(LAYOUT);
        SeatAllocationService core = new SeatAllocationService();
        assertThat(core.getAvailability(TRIP, AC, layout, repository.findCandidates(TRIP, AC), AS_OF))
                .containsExactly(state(A1, FREE, null), state(A2, UNAVAILABLE, null),
                        state(B1, UNAVAILABLE, null), state(B2, UNAVAILABLE, EXPIRES),
                        state(C1, FREE, null), state(C2, FREE, null));
        assertThat(core.getAvailability(TRIP, BC, layout, repository.findCandidates(TRIP, BC), AS_OF))
                .containsExactly(state(A1, FREE, null), state(A2, FREE, null),
                        state(B1, UNAVAILABLE, null), state(B2, UNAVAILABLE, EXPIRES),
                        state(C1, FREE, null), state(C2, FREE, null));
    }

    @Test
    void readAtExactExpiryDoesNotExtendHoldOrWriteCleanupState() {
        List<SeatAllocation> candidates = repository.findCandidates(TRIP, BC);
        assertThat(new SeatAllocationService().getAvailability(TRIP, BC, LAYOUT, candidates, EXPIRES))
                .filteredOn(s -> s.physicalSeatId().equals(B2)).containsExactly(state(B2, FREE, null));
        assertThat(repository.findCandidates(TRIP, BC)).containsExactlyInAnyOrderElementsOf(candidates);
        assertThat(jdbc.queryForObject("select status from booking where booking_id = ?", String.class,
                UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd03"))).isEqualTo("Pending");
    }

    @Test
    void fixtureBookingsHaveOneToSixDistinctSeatsAndMatchingIntegerVndTotals() {
        assertThat(jdbc.queryForObject("select count(*) from booking", Long.class)).isEqualTo(5L);
        assertThat(jdbc.queryForList("""
                select b.booking_id from booking b left join booking_seat s using (booking_id)
                group by b.booking_id, b.total_amount_vnd
                having count(s.booking_seat_id) not between 1 and 6
                    or count(s.booking_seat_id) <> count(distinct s.physical_seat_id)
                    or b.total_amount_vnd <> sum(s.fare_amount_vnd)
                """, UUID.class)).isEmpty();
        assertThat(jdbc.queryForList("select total_amount_vnd from booking order by booking_id", Long.class))
                .containsExactly(100000L, 250000L, 150000L, 250000L, 250000L);
    }

    @Test
    void allUnavailableScenarioIsIsolatedFromTheSixStateFixture() {
        jdbc.update("delete from seat_allocation");
        jdbc.update("delete from booking_seat");
        jdbc.update("delete from booking");
        jdbc.update("""
                insert into booking (booking_id, passenger_account_id, trip_id, from_stop_id, to_stop_id,
                    from_stop_index, to_stop_index, status, total_amount_vnd, hold_created_at, expires_at)
                values ('dddddddd-dddd-4ddd-8ddd-dddddddddd06',
                    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1', '11111111-1111-4111-8111-111111111111',
                    '22222222-2222-4222-8222-222222222222', '99999999-9999-4999-8999-999999999999',
                    0, 2, 'Confirmed', 1500000, '2026-11-19T00:00:00Z', '2026-11-19T00:10:00Z')
                """);
        jdbc.update("""
                insert into booking_seat
                select physical_seat_id, 'dddddddd-dddd-4ddd-8ddd-dddddddddd06', physical_seat_id,
                    'Synthetic Traveller', 250000 from physical_seat
                """);
        jdbc.update("""
                insert into seat_allocation
                select booking_seat_id, booking_seat_id, booking_id,
                    '11111111-1111-4111-8111-111111111111', physical_seat_id, 0, 2, 'Confirmed', null, null
                from booking_seat
                """);
        assertThat(jdbc.queryForObject("select count(distinct physical_seat_id) from booking_seat", Long.class))
                .isEqualTo(6L);
        assertThat(jdbc.queryForObject("select sum(fare_amount_vnd) from booking_seat", Long.class))
                .isEqualTo(1500000L);
        assertThat(new SeatAllocationService().getAvailability(TRIP, AC, LAYOUT,
                repository.findCandidates(TRIP, AC), AS_OF))
                .containsExactly(state(A1, UNAVAILABLE, null), state(A2, UNAVAILABLE, null),
                        state(B1, UNAVAILABLE, null), state(B2, UNAVAILABLE, null),
                        state(C1, UNAVAILABLE, null), state(C2, UNAVAILABLE, null));
    }

    @Test
    void incompleteLayoutIsRejectedRatherThanInventingFreeSeats() {
        assertThatThrownBy(() -> new SeatAllocationService().getAvailability(TRIP, AC, List.of(),
                repository.findCandidates(TRIP, AC), AS_OF)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void databaseReadFailurePropagatesRatherThanReturningAnEmptyCandidateList() {
        jdbc.execute("alter table seat_allocation rename to unavailable_allocation");
        assertThatThrownBy(() -> repository.findCandidates(TRIP, AC)).isInstanceOf(DataAccessException.class);
    }

    private SeatAllocationService.SeatState state(UUID seat, SeatAllocationService.SeatStatus status, Instant expiry) {
        return new SeatAllocationService.SeatState(seat, status, expiry);
    }
}
