package vn.edu.team11.coach.trip.api;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import vn.edu.team11.coach.trip.application.TripDetailsModels.Request;

@Component
public class TripDetailsRequestParser {
    public Request parse(String tripId, MultiValueMap<String, String> parameters) {
        UUID trip = uuid(tripId, "tripId");
        boolean from = parameters.containsKey("fromStopId");
        boolean to = parameters.containsKey("toStopId");
        if (from != to) throw TripDetailsException.invalid(from ? "toStopId" : "fromStopId");
        return new Request(trip, from ? scalar(parameters, "fromStopId") : null,
                to ? scalar(parameters, "toStopId") : null);
    }
    private UUID scalar(MultiValueMap<String, String> parameters, String field) {
        var values = parameters.get(field);
        if (values == null || values.size() != 1) throw TripDetailsException.invalid(field);
        return uuid(values.getFirst(), field);
    }
    private UUID uuid(String value, String field) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw TripDetailsException.invalid(field);
        }
        return UUID.fromString(value);
    }
}
