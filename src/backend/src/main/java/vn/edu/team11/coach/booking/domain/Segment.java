package vn.edu.team11.coach.booking.domain;

import java.util.Objects;

public record Segment(int fromStopIndex, int toStopIndex) {
    public Segment {
        if (fromStopIndex < 0 || toStopIndex <= fromStopIndex) {
            throw new IllegalArgumentException("Segment requires 0 <= fromStopIndex < toStopIndex");
        }
    }

    public boolean overlaps(Segment other) {
        Objects.requireNonNull(other, "other");
        return fromStopIndex < other.toStopIndex && other.fromStopIndex < toStopIndex;
    }
}
