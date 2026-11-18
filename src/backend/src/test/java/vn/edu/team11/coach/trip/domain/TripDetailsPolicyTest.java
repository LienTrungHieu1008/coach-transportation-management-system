package vn.edu.team11.coach.trip.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.*;

import static org.assertj.core.api.Assertions.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;
import static vn.edu.team11.coach.trip.TripDetailsTestData.facts;
import static vn.edu.team11.coach.trip.TripDetailsTestData.complete;
import static vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Reason.*;

class TripDetailsPolicyTest {
    private final TripDetailsPolicy policy = new TripDetailsPolicy();
    @Test
    void validSegmentAndAbsentSegmentHaveSameEligibility() {
        var selected = policy.evaluate(complete(), A, C, NOW);
        assertThat(selected.segment().fareAmountVnd()).isEqualTo(250_000L);
        assertThat(selected.segment().boardingTime()).isEqualTo(complete().stops().getFirst().departure());
        assertThat(selected.reasons()).isEmpty();
        var absent = policy.evaluate(complete(), null, null, NOW);
        assertThat(absent.segment()).isNull();
        assertThat(absent.reasons()).isEmpty();
        assertThat(absent.cutoff()).isEqualTo(CUTOFF);
    }
    @ParameterizedTest
    @EnumSource(value = TripStatus.class, names = {"Departed", "Completed", "Cancelled"})
    void closedStatusKeepsDetails(TripStatus status) {
        var result = policy.evaluate(facts(status, complete().stops(), complete().fares()), A, B, NOW);
        assertThat(result.reasons()).containsExactly(TRIP_NOT_SCHEDULED);
        assertThat(result.stops()).hasSize(3);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(100_000L);
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void cutoffEqualityIsClosed(long delta) {
        var result = policy.evaluate(complete(), B, C, CUTOFF.plusMillis(delta));
        assertThat(result.cutoff()).isEqualTo(CUTOFF);
        assertThat(result.reasons()).isEqualTo(delta < 0 ? List.of() : List.of(BOOKING_CLOSED));
    }
    @Test
    void allReasonsCombineInEnumOrder() {
        var damaged = facts(TripStatus.Cancelled, complete().stops(), List.of());
        assertThat(policy.evaluate(damaged, A, B, CUTOFF).reasons())
                .containsExactly(TRIP_NOT_SCHEDULED, BOOKING_CLOSED, ESSENTIAL_DATA_INCOMPLETE);
        assertThat(policy.evaluate(damaged, A, B, NOW).reasons())
                .containsExactly(TRIP_NOT_SCHEDULED, ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingCutoffDoesNotInventClosedReason() {
        var stops = new ArrayList<>(complete().stops());
        var first = stops.getFirst();
        stops.set(0, new StopFact(first.id(), first.name(), 0, true, true, first.departure(), null));
        var result = policy.evaluate(facts(TripStatus.Cancelled, stops, complete().fares()), A, C, CUTOFF.plusSeconds(86400));
        assertThat(result.cutoff()).isNull();
        assertThat(result.segment().boardingTime()).isNull();
        assertThat(result.reasons()).containsExactly(TRIP_NOT_SCHEDULED, ESSENTIAL_DATA_INCOMPLETE);
    }
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void everyCombinationReturnsAllKnownReasonsInOrder(int mask) {
        var trip = facts((mask & 1) == 0 ? TripStatus.Scheduled : TripStatus.Cancelled,
                complete().stops(), (mask & 4) == 0 ? complete().fares() : List.of());
        var expected = new ArrayList<Reason>();
        if ((mask & 1) != 0) expected.add(TRIP_NOT_SCHEDULED);
        if ((mask & 2) != 0) expected.add(BOOKING_CLOSED);
        if ((mask & 4) != 0) expected.add(ESSENTIAL_DATA_INCOMPLETE);
        assertThat(policy.evaluate(trip, A, B, (mask & 2) == 0 ? NOW : CUTOFF).reasons()).isEqualTo(expected);
    }
    @Test
    void knownPairSurvivesUnrelatedOrderGapWithoutRenumberingStops() {
        var stops = new ArrayList<>(complete().stops());
        var last = stops.get(2);
        stops.set(2, new StopFact(C, "C", 3, true, true, last.arrival(), last.departure()));
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, complete().fares()), A, B, NOW);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(100_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        assertThat(result.stops()).extracting(StopFact::index).containsExactly(0, 1, 3);
        assertThat(policy.evaluate(facts(TripStatus.Scheduled, stops, complete().fares()), A, C, NOW)
                .segment().fareAmountVnd()).isNull();
    }
    @Test
    void missingTripStopKeepsRouteMembershipButDoesNotDisplayInventedStop() {
        var stops = new ArrayList<>(complete().stops());
        stops.set(1, new StopFact(B, "B", 1, true, false, null, null));
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, List.of()), B, C, NOW);
        assertThat(result.stops()).extracting(StopFact::id).containsExactly(A, C);
        assertThat(result.segment()).isNotNull();
        assertThat(result.segment().boardingTime()).isNull();
        assertThat(result.segment().alightingTime()).isEqualTo(complete().stops().get(2).arrival());
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @ParameterizedTest
    @ValueSource(strings = {"AB", "BC", "AC"})
    void missingTripStopPreservesVerifiedFaresAndSegmentPriceWithoutInventingTimes(String segment) {
        var base = complete();
        var stops = new ArrayList<>(base.stops());
        stops.set(1, new StopFact(B, "B", 1, true, false, null, null));
        var from = segment.equals("BC") ? B : A;
        var to = segment.equals("AB") ? B : C;
        long expectedFare = segment.equals("AB") ? 100_000L : segment.equals("BC") ? 150_000L : 250_000L;
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, base.fares()), from, to, NOW);

        assertThat(result.stops()).extracting(StopFact::id).containsExactly(A, C);
        assertThat(result.stops()).extracting(StopFact::index).containsExactly(0, 2);
        assertThat(result.fares()).containsExactly(new Fare(A, B, 100_000), new Fare(B, C, 150_000));
        assertThat(result.segment().fromStopId()).isEqualTo(from);
        assertThat(result.segment().toStopId()).isEqualTo(to);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(expectedFare);
        assertThat(result.segment().boardingTime()).isEqualTo(from.equals(B) ? null : base.stops().getFirst().departure());
        assertThat(result.segment().alightingTime()).isEqualTo(to.equals(B) ? null : base.stops().get(2).arrival());
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "nonadjacent"})
    void missingTripStopDoesNotMakeUnverifiedFareUsable(String problem) {
        var stops = new ArrayList<>(complete().stops());
        stops.set(1, new StopFact(B, "B", 1, true, false, null, null));
        List<Fare> fares = switch (problem) {
            case "missing" -> List.of(new Fare(B, C, 150_000));
            case "invalid" -> List.of(new Fare(A, B, TripSearchPolicy.MAX_SAFE_INTEGER + 1), new Fare(B, C, 150_000));
            default -> List.of(new Fare(A, C, 250_000));
        };
        var damaged = facts(TripStatus.Scheduled, stops, fares);
        var result = policy.evaluate(damaged, A, C, NOW);
        assertThat(result.fares()).isEqualTo(problem.equals("nonadjacent") ? List.of() : List.of(new Fare(B, C, 150_000)));
        assertThat(result.segment().fareAmountVnd()).isNull();
        assertThat(policy.evaluate(damaged, A, B, NOW).segment().fareAmountVnd()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void unverifiedRouteCannotSupplyFareOrSegmentEvenWhenRawFaresExist() {
        var base = complete();
        var missingRoute = new Facts(base.tripId(), base.status(), null, false, base.vehicle(), true,
                base.capacity(), base.stops(), base.fares(), base.seats());
        var result = policy.evaluate(missingRoute, A, C, NOW);
        assertThat(result.fares()).isEmpty();
        assertThat(result.segment()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingRouteOrderPreventsSegmentVerification() {
        var base = complete();
        var missing = new Facts(base.tripId(), base.status(), null, false, base.vehicle(), true,
                base.capacity(), List.of(), List.of(), base.seats());
        var result = policy.evaluate(missing, A, B, NOW);
        assertThat(result.segment()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingFareOutsideSegmentPreservesVerifiedPrice() {
        var damaged = facts(TripStatus.Scheduled, complete().stops(), List.of(new Fare(A, B, 100_000)));
        var ab = policy.evaluate(damaged, A, B, NOW);
        assertThat(ab.segment().fareAmountVnd()).isEqualTo(100_000L);
        assertThat(ab.fares()).containsExactly(new Fare(A, B, 100_000));
        assertThat(ab.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        assertThat(policy.evaluate(damaged, A, C, NOW).segment().fareAmountVnd()).isNull();
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 9007199254740992L, Long.MAX_VALUE})
    void invalidFareIsOmittedAndSegmentPriceIsNull(long amount) {
        var damaged = facts(TripStatus.Scheduled, complete().stops(), List.of(new Fare(A, B, amount), new Fare(B, C, 150_000)));
        var result = policy.evaluate(damaged, A, C, NOW);
        assertThat(result.fares()).containsExactly(new Fare(B, C, 150_000));
        assertThat(result.segment().fareAmountVnd()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        assertThat(policy.evaluate(damaged, B, C, NOW).segment().fareAmountVnd()).isEqualTo(150_000L);
    }
    @Test
    void safeSegmentSumSupportsZeroMaxAndOverflowWithoutSummingWholeTrip() {
        var zero = facts(TripStatus.Scheduled, complete().stops(), List.of(new Fare(A, B, 0), new Fare(B, C, 0)));
        assertThat(policy.evaluate(zero, A, C, NOW).segment().fareAmountVnd()).isZero();
        long max = TripSearchPolicy.MAX_SAFE_INTEGER;
        var large = facts(TripStatus.Scheduled, complete().stops(), List.of(new Fare(A, B, max), new Fare(B, C, max)));
        assertThat(policy.evaluate(large, A, B, NOW).segment().fareAmountVnd()).isEqualTo(max);
        assertThat(policy.evaluate(large, A, B, NOW).reasons()).isEmpty();
        var overflow = policy.evaluate(large, A, C, NOW);
        assertThat(overflow.segment().fareAmountVnd()).isNull();
        assertThat(overflow.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void nonAdjacentFareCannotReplaceMissingEssentialLeg() {
        var damaged = facts(TripStatus.Scheduled, complete().stops(), List.of(new Fare(A, C, 250_000)));
        assertThat(policy.evaluate(damaged, A, C, NOW).fares()).isEmpty();
        assertThat(policy.evaluate(damaged, A, C, NOW).segment().fareAmountVnd()).isNull();
    }
    @Test
    void missingDepartureDoesNotFallbackToArrival() {
        var stops = new ArrayList<>(complete().stops());
        var b = stops.get(1);
        stops.set(1, new StopFact(B, "B", 1, true, true, b.arrival(), null));
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, complete().fares()), B, C, NOW);
        assertThat(result.segment().boardingTime()).isNull();
        assertThat(result.segment().fareAmountVnd()).isEqualTo(150_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingArrivalDoesNotFallbackToDeparture() {
        var stops = new ArrayList<>(complete().stops());
        var b = stops.get(1);
        stops.set(1, new StopFact(B, "B", 1, true, true, null, b.departure()));
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, complete().fares()), A, B, NOW);
        assertThat(result.segment().alightingTime()).isNull();
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void routeAndStopInactiveDisableEligibilityWithoutDiscardingFacts() {
        var base = complete();
        var inactiveRoute = new Facts(base.tripId(), base.status(), base.route(), false, base.vehicle(), true,
                base.capacity(), base.stops(), base.fares(), base.seats());
        assertThat(policy.evaluate(inactiveRoute, A, C, NOW).reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
        var stops = new ArrayList<>(base.stops());
        var b = stops.get(1);
        stops.set(1, new StopFact(B, "B", 1, false, true, b.arrival(), b.departure()));
        var result = policy.evaluate(facts(TripStatus.Scheduled, stops, base.fares()), A, B, NOW);
        assertThat(result.stops()).hasSize(3);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(100_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void missingLayoutStillPreservesSegmentAndPrice() {
        var base = complete();
        var damaged = new Facts(base.tripId(), base.status(), base.route(), true, base.vehicle(), true,
                base.capacity(), base.stops(), base.fares(), List.of());
        var result = policy.evaluate(damaged, A, C, NOW);
        assertThat(result.segment().fareAmountVnd()).isEqualTo(250_000L);
        assertThat(result.reasons()).containsExactly(ESSENTIAL_DATA_INCOMPLETE);
    }
    @Test
    void equalReversedAndKnownOutsideRouteAreInvalid() {
        assertThatThrownBy(() -> policy.evaluate(complete(), A, A, NOW)).isInstanceOf(InvalidSegmentException.class);
        assertThatThrownBy(() -> policy.evaluate(complete(), C, A, NOW)).isInstanceOf(InvalidSegmentException.class);
        assertThatThrownBy(() -> policy.evaluate(complete(), A, java.util.UUID.randomUUID(), NOW)).isInstanceOf(InvalidSegmentException.class);
    }
}
