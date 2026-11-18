package vn.edu.team11.coach.trip.application;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Facts;

public interface TripDetailsRepository {
    Optional<Facts> load(UUID tripId);
    List<UUID> existingStops(List<UUID> stopIds);
}
