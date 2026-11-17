package vn.edu.team11.coach.booking.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Read-side allocation facts including the owning Booking status; not the V4 persistence mapping.
public record SeatAllocation(UUID tripId, UUID physicalSeatId, Segment segment, BookingStatus bookingStatus,
                             Kind kind, Instant expiresAt, Instant releasedAt) {
    public enum Kind {
        Hold,
        Confirmed
    }

    public SeatAllocation {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(physicalSeatId, "physicalSeatId");
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(bookingStatus, "bookingStatus");
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.Hold) {
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }

    public boolean isEffective(Instant asOf) {
        Objects.requireNonNull(asOf, "asOf");
        return releasedAt == null && switch (kind) {
            case Hold -> bookingStatus == BookingStatus.Pending && asOf.isBefore(expiresAt);
            case Confirmed -> bookingStatus == BookingStatus.Confirmed;
        };
    }
}
