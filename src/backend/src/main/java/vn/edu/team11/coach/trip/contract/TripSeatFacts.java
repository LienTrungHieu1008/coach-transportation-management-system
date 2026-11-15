
package vn.edu.team11.coach.trip.contract;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TripSeatFacts(
        UUID tripId,
        UUID fromStopId,
        UUID toStopId,
        int fromStopIndex,
        int toStopIndex,
        TripStatus status,
        Instant bookingClosesAt,
        SeatLayout seatLayout,
        Instant asOf
) {
    public TripSeatFacts {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(fromStopId, "fromStopId");
        Objects.requireNonNull(toStopId, "toStopId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(bookingClosesAt, "bookingClosesAt");
        Objects.requireNonNull(seatLayout, "seatLayout");
        Objects.requireNonNull(asOf, "asOf");

        if (fromStopIndex < 0 || toStopIndex <= fromStopIndex) {
            throw new IllegalArgumentException(
                    "Invalid verified segment indices"
            );
        }
    }
}
