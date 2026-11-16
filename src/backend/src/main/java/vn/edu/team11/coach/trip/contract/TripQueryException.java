
package vn.edu.team11.coach.trip.contract;

import java.util.Objects;

public class TripQueryException extends RuntimeException {

    private final TripQueryErrorCode code;

    public TripQueryException(
            TripQueryErrorCode code,
            String message
    ) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public TripQueryException(
            TripQueryErrorCode code,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public TripQueryErrorCode getCode() {
        return code;
    }
}
