
package vn.edu.team11.coach.trip.contract;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SeatLayout(
        UUID id,
        List<LayoutSeat> seats
) {
    public SeatLayout {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(seats, "seats");

        if (seats.isEmpty()) {
            throw new IllegalArgumentException(
                    "Seat layout must contain at least one seat"
            );
        }

        Set<UUID> seatIds = new HashSet<>();
        Set<String> positions = new HashSet<>();

        for (LayoutSeat seat : seats) {
            Objects.requireNonNull(seat, "seat");

            if (!seatIds.add(seat.physicalSeatId())) {
                throw new IllegalArgumentException(
                        "Duplicate physical seat ID"
                );
            }

            String position = seat.deck()
                    + ":" + seat.row()
                    + ":" + seat.column();

            if (!positions.add(position)) {
                throw new IllegalArgumentException(
                        "Duplicate seat position"
                );
            }
        }

        Comparator<LayoutSeat> seatOrder = Comparator
                .comparingInt(LayoutSeat::deck)
                .thenComparingInt(LayoutSeat::row)
                .thenComparingInt(LayoutSeat::column)
                .thenComparing(LayoutSeat::physicalSeatId);

        seats = List.copyOf(
                seats.stream()
                        .sorted(seatOrder)
                        .toList()
        );
    }
}
