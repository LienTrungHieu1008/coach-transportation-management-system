package vn.edu.team11.coach.booking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.team11.coach.booking.domain.BookingStatus.Confirmed;
import static vn.edu.team11.coach.booking.domain.BookingStatus.Pending;
import static vn.edu.team11.coach.booking.domain.SeatAllocation.Kind.Hold;
import static vn.edu.team11.coach.booking.domain.SeatAllocationService.SeatStatus.FREE;
import static vn.edu.team11.coach.booking.domain.SeatAllocationService.SeatStatus.UNAVAILABLE;

import vn.edu.team11.coach.booking.domain.SeatAllocationService.SeatState;

class SeatAllocationServiceTests {
    private static final UUID TRIP_ID = new UUID(0, 1);
    private static final UUID OTHER_TRIP_ID = new UUID(0, 2);
    private static final UUID SEAT_ONE = new UUID(0, 11);
    private static final UUID SEAT_TWO = new UUID(0, 12);
    private static final Segment AB = new Segment(0, 1);
    private static final Segment BC = new Segment(1, 2);
    private static final Segment AC = new Segment(0, 2);
    private static final Instant AS_OF = Instant.parse("2026-11-15T00:00:00Z");
    private static final Instant EARLIER_EXPIRY = Instant.parse("2026-11-15T00:05:00Z");
    private static final Instant LATER_EXPIRY = Instant.parse("2026-11-15T00:10:00Z");

    private final SeatAllocationService service = new SeatAllocationService();

    @Test
    void returnsEverySeatInCallerOrderWhenThereAreNoAllocations() {
        var result = service.getAvailability(TRIP_ID, AC, List.of(SEAT_TWO, SEAT_ONE), List.of(), AS_OF);

        assertThat(result).containsExactly(
                new SeatState(SEAT_TWO, FREE, null),
                new SeatState(SEAT_ONE, FREE, null));
    }

    @Test
    void adjacentSegmentCanReuseSeatWhileOverlappingSegmentCannot() {
        var allocations = List.of(confirmed(TRIP_ID, SEAT_ONE, AB));

        assertThat(service.getAvailability(TRIP_ID, BC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, FREE, null));
        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, null));
        assertThat(service.getAvailability(TRIP_ID, AB, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, null));
    }

    @Test
    void allocationsFromAnotherTripDoNotBlockTheSamePhysicalSeat() {
        var allocations = List.of(confirmed(OTHER_TRIP_ID, SEAT_ONE, AC));

        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, FREE, null));
    }

    @Test
    void blockersAffectOnlyTheirOwnSeatAndAllSeatsMayBeUnavailable() {
        var firstAllocation = confirmed(TRIP_ID, SEAT_ONE, AC);
        var secondAllocation = hold(SEAT_TWO, BC, LATER_EXPIRY);

        assertThat(service.getAvailability(TRIP_ID, BC, List.of(SEAT_ONE, SEAT_TWO),
                List.of(firstAllocation), AS_OF)).containsExactly(
                new SeatState(SEAT_ONE, UNAVAILABLE, null),
                new SeatState(SEAT_TWO, FREE, null));
        assertThat(service.getAvailability(TRIP_ID, BC, List.of(SEAT_ONE, SEAT_TWO),
                List.of(firstAllocation, secondAllocation), AS_OF)).containsExactly(
                new SeatState(SEAT_ONE, UNAVAILABLE, null),
                new SeatState(SEAT_TWO, UNAVAILABLE, LATER_EXPIRY));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usesEarliestExpiryAcrossBlockingHoldSegmentsRegardlessOfInputOrder(boolean reverse) {
        var firstLeg = hold(SEAT_ONE, AB, LATER_EXPIRY);
        var secondLeg = hold(SEAT_ONE, BC, EARLIER_EXPIRY);
        var allocations = reverse ? List.of(secondLeg, firstLeg) : List.of(firstLeg, secondLeg);

        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, EARLIER_EXPIRY));
    }

    @Test
    void refreshHintExcludesHoldsOutsideTheRequestedSegment() {
        var allocations = List.of(hold(SEAT_ONE, AB, EARLIER_EXPIRY), hold(SEAT_ONE, BC, LATER_EXPIRY));

        assertThat(service.getAvailability(TRIP_ID, BC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, LATER_EXPIRY));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void confirmedBlockerSuppressesHoldRefreshHintRegardlessOfInputOrder(boolean reverse) {
        var firstLeg = confirmed(TRIP_ID, SEAT_ONE, AB);
        var secondLeg = hold(SEAT_ONE, BC, EARLIER_EXPIRY);
        var allocations = reverse ? List.of(secondLeg, firstLeg) : List.of(firstLeg, secondLeg);

        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, null));
    }

    @Test
    void expiredCancelledReleasedAndMismatchedAllocationsDoNotBlock() {
        var allocations = List.of(
                hold(SEAT_ONE, AC, AS_OF),
                hold(SEAT_ONE, AC, AS_OF.minusSeconds(1)),
                new SeatAllocation(TRIP_ID, SEAT_ONE, AC, BookingStatus.Expired, Hold, LATER_EXPIRY, null),
                new SeatAllocation(TRIP_ID, SEAT_ONE, AC, BookingStatus.Cancelled,
                        SeatAllocation.Kind.Confirmed, null, null),
                new SeatAllocation(TRIP_ID, SEAT_ONE, AC, Confirmed,
                        SeatAllocation.Kind.Confirmed, null, AS_OF.minusSeconds(1)),
                new SeatAllocation(TRIP_ID, SEAT_ONE, AC, Pending,
                        SeatAllocation.Kind.Confirmed, null, null),
                new SeatAllocation(TRIP_ID, SEAT_ONE, AC, Confirmed, Hold, LATER_EXPIRY, null));

        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, FREE, null));
    }

    @Test
    void reevaluatesExpiryWithoutExtendingHoldOrChangingBookingState() {
        var allocation = hold(SEAT_ONE, AC, EARLIER_EXPIRY);
        var allocations = List.of(allocation);

        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF))
                .containsExactly(new SeatState(SEAT_ONE, UNAVAILABLE, EARLIER_EXPIRY));
        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, EARLIER_EXPIRY))
                .containsExactly(new SeatState(SEAT_ONE, FREE, null));
        assertThat(service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations,
                EARLIER_EXPIRY.plusNanos(1))).containsExactly(new SeatState(SEAT_ONE, FREE, null));
        assertThat(allocation.bookingStatus()).isEqualTo(Pending);
        assertThat(allocation.expiresAt()).isEqualTo(EARLIER_EXPIRY);
    }

    @Test
    void returnsAnImmutableSnapshotWithoutMutatingOrRetainingCallerLists() {
        var seatIds = new ArrayList<>(List.of(SEAT_TWO, SEAT_ONE));
        var allocations = new ArrayList<>(List.of(hold(SEAT_ONE, AC, LATER_EXPIRY)));

        var result = service.getAvailability(TRIP_ID, AC, seatIds, allocations, AS_OF);

        assertThat(seatIds).containsExactly(SEAT_TWO, SEAT_ONE);
        assertThat(allocations).containsExactly(hold(SEAT_ONE, AC, LATER_EXPIRY));
        assertThat(service.getAvailability(TRIP_ID, AC, seatIds, allocations, AS_OF)).isEqualTo(result);
        assertThatThrownBy(() -> result.clear()).isInstanceOf(UnsupportedOperationException.class);

        seatIds.clear();
        allocations.clear();
        assertThat(result).containsExactly(
                new SeatState(SEAT_TWO, FREE, null),
                new SeatState(SEAT_ONE, UNAVAILABLE, LATER_EXPIRY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tripId", "segment", "physicalSeatIds", "allocations", "asOf"})
    void rejectsMissingQueryInput(String missing) {
        assertThatNullPointerException().isThrownBy(() -> service.getAvailability(
                missing.equals("tripId") ? null : TRIP_ID,
                missing.equals("segment") ? null : AC,
                missing.equals("physicalSeatIds") ? null : List.of(SEAT_ONE),
                missing.equals("allocations") ? null : List.of(),
                missing.equals("asOf") ? null : AS_OF));
    }

    @Test
    void rejectsEmptySeatLayout() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> service.getAvailability(TRIP_ID, AC, List.of(), List.of(), AS_OF));
    }

    @Test
    void rejectsDuplicateSeatIdsInsteadOfSilentlyDeduplicating() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.getAvailability(
                TRIP_ID, AC, List.of(SEAT_ONE, SEAT_ONE), List.of(), AS_OF));
    }

    @Test
    void rejectsNullSeatIds() {
        assertThatNullPointerException().isThrownBy(() -> service.getAvailability(
                TRIP_ID, AC, Arrays.asList(SEAT_ONE, null), List.of(), AS_OF));
    }

    @Test
    void rejectsNullAllocationEvenWhenAnotherAllocationAlreadyBlocksSeat() {
        var allocations = Arrays.asList(confirmed(TRIP_ID, SEAT_ONE, AC), null);

        assertThatNullPointerException().isThrownBy(
                () -> service.getAvailability(TRIP_ID, AC, List.of(SEAT_ONE), allocations, AS_OF));
    }

    private SeatAllocation confirmed(UUID tripId, UUID seatId, Segment segment) {
        return new SeatAllocation(tripId, seatId, segment, Confirmed,
                SeatAllocation.Kind.Confirmed, null, null);
    }

    private SeatAllocation hold(UUID seatId, Segment segment, Instant expiresAt) {
        return new SeatAllocation(TRIP_ID, seatId, segment, Pending, Hold, expiresAt, null);
    }
}
