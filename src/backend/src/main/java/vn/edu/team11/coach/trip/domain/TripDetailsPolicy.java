package vn.edu.team11.coach.trip.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.SeatLayout;
import vn.edu.team11.coach.trip.contract.TripStatus;

/** P05 preserves verified facts even when the Trip cannot be booked. */
public final class TripDetailsPolicy {
    public record Reference(UUID id, String name) { }
    public record Vehicle(UUID id, String vehicleType, UUID seatLayoutId) { }
    public record StopFact(UUID id, String name, int index, boolean active, boolean configured,
                           Instant arrival, Instant departure) { }
    public record Fare(UUID fromStopId, UUID toStopId, long fareAmountVnd) { }
    public record Facts(UUID tripId, TripStatus status, Reference route, boolean routeActive,
                        Vehicle vehicle, boolean vehicleActive, int capacity,
                        List<StopFact> stops, List<Fare> fares, List<LayoutSeat> seats) {
        public Facts { stops = List.copyOf(stops); fares = List.copyOf(fares); seats = List.copyOf(seats); }
    }
    public record Segment(UUID fromStopId, UUID toStopId, Instant boardingTime,
                          Instant alightingTime, Long fareAmountVnd) { }
    public enum Reason { TRIP_NOT_SCHEDULED, BOOKING_CLOSED, ESSENTIAL_DATA_INCOMPLETE }
    public record Result(Facts facts, List<StopFact> stops, List<Fare> fares, Segment segment,
                         Instant cutoff, List<Reason> reasons, Instant asOf) {
        public Result { stops = List.copyOf(stops); fares = List.copyOf(fares); reasons = List.copyOf(reasons); }
    }
    private record Leg(UUID from, UUID to) { }
    /** Segment invalidity is distinct from configuration which prevents verification. */
    public static final class InvalidSegmentException extends RuntimeException { }

    public Result evaluate(Facts facts, UUID fromId, UUID toId, Instant asOf) {
        var stops = facts.stops().stream().sorted(Comparator.comparingInt(StopFact::index)).toList();
        boolean ordered = facts.route() != null && stops.size() >= 2;
        for (int i = 0; i < stops.size(); i++) {
            StopFact stop = stops.get(i);
            ordered &= stop.index() == i && stop.id() != null && nonblank(stop.name());
        }
        boolean complete = ordered && facts.routeActive();
        for (int i = 0; i < stops.size(); i++) {
            StopFact stop = stops.get(i);
            complete &= stop.active() && stop.configured();
            if (i < stops.size() - 1) complete &= stop.departure() != null;
            if (i > 0) {
                Instant previous = stops.get(i - 1).departure();
                complete &= stop.arrival() != null && previous != null && stop.arrival().isAfter(previous);
            }
            if (stop.arrival() != null && stop.departure() != null) {
                complete &= !stop.arrival().isAfter(stop.departure());
            }
        }
        StopFact first = stops.stream().filter(stop -> stop.index() == 0).findFirst().orElse(null);
        Instant cutoff = facts.route() == null || first == null || !first.configured() || first.departure() == null
                ? null : first.departure().minusSeconds(1800);
        complete &= cutoff != null && validLayout(facts);

        Map<Leg, Long> amounts = new HashMap<>();
        for (Fare fare : facts.fares()) {
            if (fare.fareAmountVnd() >= 0 && fare.fareAmountVnd() <= TripSearchPolicy.MAX_SAFE_INTEGER) {
                amounts.put(new Leg(fare.fromStopId(), fare.toStopId()), fare.fareAmountVnd());
            }
        }
        var verifiedFares = new ArrayList<Fare>();
        // Fare verification uses RouteStop adjacency, not TripStop presence.
        // Missing TripStop still disables eligibility and provides no timestamp.
        for (int i = 0; i + 1 < stops.size(); i++) {
            StopFact from = stops.get(i);
            StopFact to = stops.get(i + 1);
            Long amount = amounts.get(new Leg(from.id(), to.id()));
            boolean verified = facts.route() != null && from.id() != null && to.id() != null
                    && from.index() >= 0 && to.index() == from.index() + 1
                    && nonblank(from.name()) && nonblank(to.name()) && amount != null;
            complete &= verified;
            if (verified) verifiedFares.add(new Fare(from.id(), to.id(), amount));
        }
        Map<Leg, Long> verifiedAmounts = new HashMap<>();
        verifiedFares.forEach(fare -> verifiedAmounts.put(new Leg(fare.fromStopId(), fare.toStopId()), fare.fareAmountVnd()));
        Segment segment = null;
        if (fromId != null) {
            StopFact from = find(stops, fromId);
            StopFact to = find(stops, toId);
            boolean pairKnown = facts.route() != null && from != null && to != null
                    && nonblank(from.name()) && nonblank(to.name()) && from.index() >= 0 && to.index() >= 0;
            if (fromId.equals(toId) || (pairKnown && from.index() >= to.index())) {
                throw new InvalidSegmentException();
            }
            if (ordered && (from == null || to == null)) throw new InvalidSegmentException();
            if (pairKnown) {
                Long sum = 0L;
                for (int i = stops.indexOf(from); i < stops.indexOf(to); i++) {
                    Long amount = verifiedAmounts.get(new Leg(stops.get(i).id(), stops.get(i + 1).id()));
                    if (amount == null || sum > TripSearchPolicy.MAX_SAFE_INTEGER - amount) {
                        sum = null;
                        complete = false;
                        break;
                    }
                    sum = Math.addExact(sum, amount);
                }
                segment = new Segment(fromId, toId, from.configured() ? from.departure() : null,
                        to.configured() ? to.arrival() : null, sum);
            } else {
                complete = false;
            }
        }
        var reasons = new ArrayList<Reason>();
        if (facts.status() != TripStatus.Scheduled) reasons.add(Reason.TRIP_NOT_SCHEDULED);
        if (cutoff != null && !asOf.isBefore(cutoff)) reasons.add(Reason.BOOKING_CLOSED);
        if (!complete) reasons.add(Reason.ESSENTIAL_DATA_INCOMPLETE);
        var displayedStops = stops.stream().filter(stop -> stop.configured() && stop.id() != null
                && nonblank(stop.name()) && stop.index() >= 0).toList();
        return new Result(facts, displayedStops, verifiedFares, segment, cutoff, reasons, asOf);
    }

    private boolean validLayout(Facts facts) {
        Vehicle vehicle = facts.vehicle();
        if (vehicle == null || !facts.vehicleActive() || !nonblank(vehicle.vehicleType())
                || facts.capacity() <= 0 || facts.seats().size() != facts.capacity()) return false;
        try {
            new SeatLayout(vehicle.seatLayoutId(), facts.seats());
            return true;
        } catch (IllegalArgumentException | NullPointerException failure) {
            return false;
        }
    }

    private static StopFact find(List<StopFact> stops, UUID id) {
        return stops.stream().filter(stop -> id.equals(stop.id())).findFirst().orElse(null);
    }
    private static boolean nonblank(String value) { return value != null && !value.isBlank(); }
}
