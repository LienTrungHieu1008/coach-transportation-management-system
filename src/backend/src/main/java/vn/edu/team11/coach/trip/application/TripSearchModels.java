package vn.edu.team11.coach.trip.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.team11.coach.trip.domain.TripSearchPolicy.Candidate;

public final class TripSearchModels {
    private TripSearchModels() { }

    public record Request(UUID fromStopId, UUID toStopId, LocalDate boardingDate, int page, int size) { }
    public record Reference(UUID id, String name) { }
    public record Item(UUID tripId, Reference route, Reference fromStop, Reference toStop,
                       Instant boardingTime, Instant alightingTime, String vehicleType,
                       long fareAmountVnd, Instant bookingClosesAt) { }
    public record Page(List<Item> items, int page, int size, long totalElements,
                       long totalPages, Instant serverTime) {
        public Page { items = List.copyOf(items); }
    }
    public record ReadData(boolean fromServed, boolean toServed, boolean orderedRouteExists,
                           List<Candidate> candidates) {
        public ReadData { candidates = List.copyOf(candidates); }
    }
}
