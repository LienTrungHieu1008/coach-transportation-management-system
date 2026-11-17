package vn.edu.team11.coach.trip.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.edu.team11.coach.trip.domain.TripSearchPolicy.Candidate;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.IncompleteTripException;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.LegFare;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.StopFact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

class TripSearchPolicyTest {
    private final TripSearchPolicy policy = new TripSearchPolicy();

    @Test
    void sumsOnlySelectedConsecutiveLegs() {
        assertThat(policy.evaluate(complete(), A, C, DATE, NOW).orElseThrow().fare()).isEqualTo(250_000);
        assertThat(policy.evaluate(complete(), B, C, DATE, NOW).orElseThrow().fare()).isEqualTo(150_000);
    }

    @Test
    void exactCutoffIsClosedAndInstantBeforeIsOpen() {
        assertThat(policy.evaluate(complete(), A, B, DATE, CUTOFF.minusMillis(1))).isPresent();
        assertThat(policy.evaluate(complete(), A, B, DATE, CUTOFF)).isEmpty();
        assertThat(policy.evaluate(complete(), A, B, DATE, CUTOFF.plusMillis(1))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cancelled", "Departed", "Completed"})
    void nonscheduledIncompleteTripsAreExcluded(String status) {
        assertThat(policy.evaluate(trip(List.of(), List.of(), status), A, B, DATE, NOW)).isEmpty();
    }

    @Test
    void knownWrongDateOrClosedTripDoesNotFailForMissingFares() {
        Candidate missing = trip(stops(), List.of(), "Scheduled");
        assertThat(policy.evaluate(missing, A, B, DATE.plusDays(1), NOW)).isEmpty();
        assertThat(policy.evaluate(missing, A, B, DATE, CUTOFF)).isEmpty();
    }

    @Test
    void missingFareOutsideSegmentStillInvalidatesWholeTrip() {
        var missing = trip(stops(), List.of(new LegFare(A, B, 100_000)), "Scheduled");
        assertThatThrownBy(() -> policy.evaluate(missing, A, B, DATE, NOW)).isInstanceOf(IncompleteTripException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 9007199254740992L, Long.MAX_VALUE})
    void invalidFareAnywhereOnTripIsRejected(long amount) {
        var invalid = trip(stops(), List.of(new LegFare(A, B, 100_000), new LegFare(B, C, amount)), "Scheduled");
        assertThatThrownBy(() -> policy.evaluate(invalid, A, B, DATE, NOW)).isInstanceOf(IncompleteTripException.class);
    }

    @Test
    void zeroFareAndMaximumSafeSegmentAreValidWithoutSummingWholeTrip() {
        var zero = trip(stops(), List.of(new LegFare(A, B, 0), new LegFare(B, C, 0)), "Scheduled");
        assertThat(policy.evaluate(zero, A, C, DATE, NOW).orElseThrow().fare()).isZero();
        var large = trip(stops(), List.of(new LegFare(A, B, TripSearchPolicy.MAX_SAFE_INTEGER),
                new LegFare(B, C, TripSearchPolicy.MAX_SAFE_INTEGER)), "Scheduled");
        assertThat(policy.evaluate(large, A, B, DATE, NOW).orElseThrow().fare()).isEqualTo(TripSearchPolicy.MAX_SAFE_INTEGER);
        assertThatThrownBy(() -> policy.evaluate(large, A, C, DATE, NOW)).isInstanceOf(IncompleteTripException.class);
    }

    @Test
    void noFallbackForMissingBoardingOrAlightingTimestamp() {
        for (boolean boarding : List.of(true, false)) {
            var facts = new ArrayList<>(stops());
            StopFact middle = facts.get(1);
            facts.set(1, new StopFact(B, "B", 1, true, boarding ? middle.arrival() : null,
                    boarding ? null : middle.departure()));
            Candidate invalid = trip(facts, fares(), "Scheduled");
            assertThatThrownBy(() -> policy.evaluate(invalid, boarding ? B : A, boarding ? C : B, DATE, NOW))
                    .isInstanceOf(IncompleteTripException.class);
        }
    }

    @Test
    void missingCutoffOrUnconfiguredStopCannotProduceSellableItem() {
        var facts = new ArrayList<>(stops());
        facts.set(0, new StopFact(A, "A", 0, true, NOW, null));
        assertThatThrownBy(() -> policy.evaluate(trip(facts, fares(), "Scheduled"), B, C, DATE, NOW))
                .isInstanceOf(IncompleteTripException.class);
        facts.set(0, new StopFact(A, "A", 0, false, null, null));
        assertThatThrownBy(() -> policy.evaluate(trip(facts, fares(), "Scheduled"), A, B, DATE, NOW))
                .isInstanceOf(IncompleteTripException.class);
    }

    @Test
    void missingLayoutDisablesOtherwiseMatchingTrip() {
        Candidate good = complete();
        Candidate invalid = new Candidate(good.id(), good.routeId(), good.routeName(), good.status(),
                good.vehicleId(), good.vehicleType(), good.vehicleActive(), good.layoutId(), good.capacity(),
                good.stops(), good.fares(), List.of());
        assertThatThrownBy(() -> policy.evaluate(invalid, A, B, DATE, NOW)).isInstanceOf(IncompleteTripException.class);
    }
}
