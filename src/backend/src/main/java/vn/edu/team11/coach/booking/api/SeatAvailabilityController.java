package vn.edu.team11.coach.booking.api;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.team11.coach.booking.application.SeatAvailabilityApplicationService;

@RestController
public class SeatAvailabilityController {
    private final SeatAvailabilityApplicationService service;

    public SeatAvailabilityController(SeatAvailabilityApplicationService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/trips/{tripId}/seat-availability")
    public ResponseEntity<SeatAvailabilityResponse> getSeatAvailability(@PathVariable String tripId,
                                                                       @RequestParam MultiValueMap<String, String> query) {
        UUID trip = canonicalUuid(tripId, "tripId");
        UUID from = requiredUuid(query, "fromStopId");
        UUID to = requiredUuid(query, "toStopId");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getSeatAvailability(trip, from, to));
    }

    private static UUID requiredUuid(MultiValueMap<String, String> query, String field) {
        var values = query.get(field);
        if (values == null || values.size() != 1) {
            throw new InvalidRequestException(field);
        }
        return canonicalUuid(values.getFirst(), field);
    }

    private static UUID canonicalUuid(String value, String field) {
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) {
                throw new InvalidRequestException(field);
            }
            return id;
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new InvalidRequestException(field);
        }
    }

    static final class InvalidRequestException extends RuntimeException {
        private final String field;

        InvalidRequestException(String field) {
            this.field = field;
        }

        String field() {
            return field;
        }
    }
}
