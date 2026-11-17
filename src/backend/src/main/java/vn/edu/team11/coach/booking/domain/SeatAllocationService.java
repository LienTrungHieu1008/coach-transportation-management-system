package vn.edu.team11.coach.booking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SeatAllocationService {
    public enum SeatStatus {
        FREE,
        UNAVAILABLE
    }

    public record SeatState(UUID physicalSeatId, SeatStatus status, Instant expiresAt) {
    }

    public List<SeatState> getAvailability(UUID tripId, Segment segment, List<UUID> physicalSeatIds,
                                         List<SeatAllocation> allocations, Instant asOf) {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(physicalSeatIds, "physicalSeatIds");
        Objects.requireNonNull(allocations, "allocations");
        Objects.requireNonNull(asOf, "asOf");
        if (physicalSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Seat layout must contain at least one seat");
        }

        Set<UUID> seatIds = new HashSet<>();
        for (UUID seatId : physicalSeatIds) {
            Objects.requireNonNull(seatId, "physicalSeatId");
            if (!seatIds.add(seatId)) {
                throw new IllegalArgumentException("Duplicate physical seat ID");
            }
        }

        Map<UUID, List<SeatAllocation>> blockers = new HashMap<>();
        for (SeatAllocation allocation : allocations) {
            Objects.requireNonNull(allocation, "allocation");
            if (tripId.equals(allocation.tripId()) && seatIds.contains(allocation.physicalSeatId())
                    && segment.overlaps(allocation.segment()) && allocation.isEffective(asOf)) {
                blockers.computeIfAbsent(allocation.physicalSeatId(), id -> new ArrayList<>()).add(allocation);
            }
        }

        return physicalSeatIds.stream().map(seatId -> {
            List<SeatAllocation> seatBlockers = blockers.getOrDefault(seatId, List.of());
            boolean confirmed = seatBlockers.stream()
                    .anyMatch(allocation -> allocation.kind() == SeatAllocation.Kind.Confirmed);
            Instant expiresAt = confirmed ? null : seatBlockers.stream()
                    .map(SeatAllocation::expiresAt).min(Instant::compareTo).orElse(null);
            return new SeatState(seatId, seatBlockers.isEmpty() ? SeatStatus.FREE : SeatStatus.UNAVAILABLE, expiresAt);
        }).toList();
    }
}
