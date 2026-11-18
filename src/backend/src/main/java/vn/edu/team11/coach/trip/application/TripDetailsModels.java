package vn.edu.team11.coach.trip.application;

import java.util.UUID;

public final class TripDetailsModels {
    private TripDetailsModels() { }
    public record Request(UUID tripId, UUID fromStopId, UUID toStopId) { }
}
