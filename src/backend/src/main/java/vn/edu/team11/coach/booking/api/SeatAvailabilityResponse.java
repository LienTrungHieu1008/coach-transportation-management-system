package vn.edu.team11.coach.booking.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import vn.edu.team11.coach.trip.contract.SeatLayout;

public record SeatAvailabilityResponse(UUID tripId, UUID fromStopId, UUID toStopId,
                                       @JsonFormat(shape = JsonFormat.Shape.STRING) Instant serverTime,
                                       @JsonFormat(shape = JsonFormat.Shape.STRING) Instant bookingClosesAt,
                                       SeatLayout layout, List<SeatState> seats) {
    public SeatAvailabilityResponse {
        seats = List.copyOf(seats);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SeatState(UUID physicalSeatId, String status,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt) {
    }
}
