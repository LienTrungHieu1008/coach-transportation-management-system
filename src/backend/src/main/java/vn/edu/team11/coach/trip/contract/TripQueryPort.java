
package vn.edu.team11.coach.trip.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Public read contract from Trip & Operations to Booking & Payment.
 * Does not create holds or allocate seats.
 */
public interface TripQueryPort {

    TripSeatFacts getSeatAvailabilityFacts(
            UUID tripId,
            UUID fromStopId,
            UUID toStopId,
            Instant asOf
    );
}
