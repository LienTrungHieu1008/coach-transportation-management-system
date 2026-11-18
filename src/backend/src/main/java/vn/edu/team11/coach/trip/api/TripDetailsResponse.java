package vn.edu.team11.coach.trip.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.*;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record TripDetailsResponse(UUID tripId, TripStatus status, Reference route, Vehicle vehicle,
                                  List<TripStop> stops, List<Fare> legFares, String description,
                                  SelectedSegment selectedSegment, Instant bookingClosesAt,
                                  Eligibility bookingEligibility, Instant serverTime) {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TripStop(Reference stop, int index, Instant plannedArrivalTime, Instant plannedDepartureTime) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SelectedSegment(UUID fromStopId, UUID toStopId, Instant boardingTime,
                                  Instant alightingTime, Long fareAmountVnd) { }
    public record Eligibility(boolean allowed, List<Reason> reasons) { }
    public static TripDetailsResponse from(Result result) {
        Segment segment = result.segment();
        return new TripDetailsResponse(result.facts().tripId(), result.facts().status(), result.facts().route(),
                result.facts().vehicle(), result.stops().stream().map(stop -> new TripStop(new Reference(stop.id(), stop.name()),
                stop.index(), stop.arrival(), stop.departure())).toList(), result.fares(), null,
                segment == null ? null : new SelectedSegment(segment.fromStopId(), segment.toStopId(), segment.boardingTime(),
                        segment.alightingTime(), segment.fareAmountVnd()), result.cutoff(),
                new Eligibility(result.reasons().isEmpty(), result.reasons()), result.asOf());
    }
}
