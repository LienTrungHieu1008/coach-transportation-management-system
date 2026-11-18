package vn.edu.team11.coach.trip.application;

import java.util.List;

public final class TripDetailsException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<FieldError> fieldErrors;
    public record FieldError(String field, String message) { }

    private TripDetailsException(int status, String code, String message, List<FieldError> fields, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fields);
    }
    public static TripDetailsException invalid(String field) {
        return new TripDetailsException(400, "INVALID_REQUEST", "Request parameters are invalid.",
                List.of(new FieldError(field, "Supply one UUID; Stop parameters must be supplied together.")), null);
    }
    public static TripDetailsException segment() {
        return new TripDetailsException(422, "INVALID_SEGMENT", "Boarding Stop must precede alighting Stop on this Trip.",
                List.of(new FieldError("toStopId", "Select a valid later Stop on this Trip.")), null);
    }
    public static TripDetailsException stopNotServed(String field) {
        return new TripDetailsException(422, "STOP_NOT_SERVED", "The selected Stop does not exist.",
                List.of(new FieldError(field, "Select an existing Stop.")), null);
    }
    public static TripDetailsException notFound() {
        return new TripDetailsException(404, "TRIP_NOT_FOUND", "Trip is unavailable.", List.of(), null);
    }
    public static TripDetailsException unavailable(Throwable cause) {
        return new TripDetailsException(503, "TRIP_DATA_UNAVAILABLE",
                "Trip information could not be verified. Please retry.", List.of(), cause);
    }
    public int status() { return status; }
    public String code() { return code; }
    public List<FieldError> fieldErrors() { return fieldErrors; }
}
