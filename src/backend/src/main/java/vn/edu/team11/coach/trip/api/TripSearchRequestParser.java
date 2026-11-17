package vn.edu.team11.coach.trip.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import vn.edu.team11.coach.trip.application.TripSearchException;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;

@Component
public class TripSearchRequestParser {
    public Request parse(MultiValueMap<String, String> parameters) {
        UUID from = uuid(value(parameters, "fromStopId", null), "fromStopId");
        UUID to = uuid(value(parameters, "toStopId", null), "toStopId");
        String rawDate = value(parameters, "boardingDate", null);
        LocalDate date;
        try {
            if (!rawDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw TripSearchException.invalid("boardingDate");
            date = LocalDate.parse(rawDate);
        } catch (DateTimeParseException exception) {
            throw TripSearchException.invalid("boardingDate");
        }
        return new Request(from, to, date, integer(value(parameters, "page", "0"), "page", 0, Integer.MAX_VALUE),
                integer(value(parameters, "size", "20"), "size", 1, 100));
    }

    private String value(MultiValueMap<String, String> parameters, String field, String defaultValue) {
        if (!parameters.containsKey(field)) {
            if (defaultValue != null) return defaultValue;
            throw TripSearchException.invalid(field);
        }
        var values = parameters.get(field);
        if (values == null || values.size() != 1 || values.getFirst() == null || values.getFirst().isBlank()) {
            throw TripSearchException.invalid(field);
        }
        return values.getFirst();
    }

    private UUID uuid(String value, String field) {
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw TripSearchException.invalid(field);
        }
        return UUID.fromString(value);
    }

    private int integer(String value, String field, int minimum, int maximum) {
        try {
            if (!value.matches("[0-9]+")) throw TripSearchException.invalid(field);
            int number = Integer.parseInt(value);
            if (number < minimum || number > maximum) throw TripSearchException.invalid(field);
            return number;
        } catch (NumberFormatException exception) {
            throw TripSearchException.invalid(field);
        }
    }
}
