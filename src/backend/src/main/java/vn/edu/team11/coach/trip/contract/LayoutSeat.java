
package vn.edu.team11.coach.trip.contract;

import java.util.Objects;
import java.util.UUID;

public record LayoutSeat(
        UUID physicalSeatId,
        String label,
        int deck,
        int row,
        int column
) {
    public LayoutSeat {
        Objects.requireNonNull(physicalSeatId, "physicalSeatId");
        Objects.requireNonNull(label, "label");

        if (label.isBlank()) {
            throw new IllegalArgumentException("Seat label must not be blank");
        }

        if (deck < 0 || row < 0 || column < 0) {
            throw new IllegalArgumentException(
                    "Seat coordinates must be non-negative"
            );
        }
    }
}
