package vn.edu.team11.coach.trip.application;

import java.util.List;

public class TripSearchException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<FieldError> fieldErrors;

    private TripSearchException(int status, String code, String message,
                                List<FieldError> fieldErrors, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public static TripSearchException invalid(String field) {
        return new TripSearchException(400, "INVALID_REQUEST", "Request parameters are invalid.",
                List.of(new FieldError(field, "Supply one valid value within the allowed range.")), null);
    }

    public static TripSearchException criteria(String code, String field, String message) {
        return new TripSearchException(422, code, message, List.of(new FieldError(field, message)), null);
    }

    public static TripSearchException unavailable(Throwable cause) {
        return new TripSearchException(503, "TRIP_DATA_UNAVAILABLE",
                "Trip information could not be verified. Please retry.", List.of(), cause);
    }

    public int status() { return status; }
    public String code() { return code; }
    public List<FieldError> fieldErrors() { return fieldErrors; }
    public record FieldError(String field, String message) { }
}
