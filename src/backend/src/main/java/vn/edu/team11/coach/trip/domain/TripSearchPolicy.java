package vn.edu.team11.coach.trip.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.SeatLayout;

public final class TripSearchPolicy {
    public static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    public static final ZoneId BOARDING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public record StopFact(UUID id, String name, int index, boolean configured,
                           Instant arrival, Instant departure) { }
    public record LegFare(UUID from, UUID to, long amount) { }
    public record Candidate(UUID id, UUID routeId, String routeName, String status,
                            UUID vehicleId, String vehicleType, boolean vehicleActive,
                            UUID layoutId, int capacity, List<StopFact> stops,
                            List<LegFare> fares, List<LayoutSeat> seats) { }
    public record Verified(Candidate trip, StopFact from, StopFact to, long fare, Instant cutoff) { }
    private record LegKey(UUID from, UUID to) { }

    public static class IncompleteTripException extends RuntimeException {
        IncompleteTripException() { super("Essential Trip data is incomplete or invalid"); }
    }

    public Optional<Verified> evaluate(Candidate trip, UUID fromId, UUID toId,
                                       LocalDate boardingDate, Instant asOf) {
        if (!"Scheduled".equals(trip.status())) return Optional.empty();
        StopFact from = find(trip.stops(), fromId);
        StopFact to = find(trip.stops(), toId);
        StopFact first = trip.stops().stream().filter(stop -> stop.index() == 0).findFirst().orElse(null);
        // Known exclusions take precedence over missing unrelated configuration.
        if (from != null && from.departure() != null
                && !from.departure().atZone(BOARDING_ZONE).toLocalDate().equals(boardingDate)) {
            return Optional.empty();
        }
        Instant cutoff = first == null || first.departure() == null
                ? null : first.departure().minusSeconds(1800);
        if (cutoff != null && !asOf.isBefore(cutoff)) return Optional.empty();

        require(from != null && to != null && from.index() < to.index() && cutoff != null);
        require(trip.stops().size() >= 2 && nonblank(trip.routeName()));
        for (int index = 0; index < trip.stops().size(); index++) {
            StopFact stop = trip.stops().get(index);
            require(stop.configured() && stop.index() == index && nonblank(stop.name()));
            if (index < trip.stops().size() - 1) require(stop.departure() != null);
            if (index > 0) {
                require(stop.arrival() != null);
                require(stop.arrival().isAfter(trip.stops().get(index - 1).departure()));
            }
            if (stop.arrival() != null && stop.departure() != null) {
                require(!stop.arrival().isAfter(stop.departure()));
            }
        }
        require(trip.vehicleId() != null && trip.vehicleActive() && nonblank(trip.vehicleType()));
        require(trip.layoutId() != null && trip.capacity() > 0 && trip.seats().size() == trip.capacity());
        try {
            new SeatLayout(trip.layoutId(), trip.seats());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IncompleteTripException();
        }

        Map<LegKey, Long> fares = new HashMap<>();
        for (LegFare fare : trip.fares()) {
            require(fare.amount() >= 0 && fare.amount() <= MAX_SAFE_INTEGER);
            require(fares.put(new LegKey(fare.from(), fare.to()), fare.amount()) == null);
        }
        long segmentFare = 0;
        for (int index = 0; index < trip.stops().size() - 1; index++) {
            Long fare = fares.get(new LegKey(trip.stops().get(index).id(), trip.stops().get(index + 1).id()));
            require(fare != null);
            if (index >= from.index() && index < to.index()) {
                require(segmentFare <= MAX_SAFE_INTEGER - fare);
                segmentFare = Math.addExact(segmentFare, fare);
            }
        }
        // Only the requested Segment is summed; full-Trip fare coverage is still mandatory.
        return Optional.of(new Verified(trip, from, to, segmentFare, cutoff));
    }

    private static StopFact find(List<StopFact> stops, UUID id) {
        return stops.stream().filter(stop -> stop.id().equals(id)).findFirst().orElse(null);
    }

    private static boolean nonblank(String value) { return value != null && !value.isBlank(); }
    private static void require(boolean condition) {
        if (!condition) throw new IncompleteTripException();
    }
}
