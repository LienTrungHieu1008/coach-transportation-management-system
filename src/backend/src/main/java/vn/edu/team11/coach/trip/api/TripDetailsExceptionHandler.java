package vn.edu.team11.coach.trip.api;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import vn.edu.team11.coach.trip.application.TripDetailsException.FieldError;

@RestControllerAdvice(assignableTypes = TripDetailsController.class)
public class TripDetailsExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TripDetailsExceptionHandler.class);
    public record ApiError(String code, String message, List<FieldError> fieldErrors) { }
    @ExceptionHandler(TripDetailsException.class)
    public ResponseEntity<ApiError> detailsFailure(TripDetailsException failure) {
        if (failure.status() == 503) LOG.error("Trip details retrieval failed", failure);
        return error(failure.status(), failure.code(), failure.getMessage(), failure.fieldErrors());
    }
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ApiError> retrievalFailure(Exception failure) {
        return detailsFailure(TripDetailsException.unavailable(failure));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception failure) {
        LOG.error("Unexpected Trip details failure", failure);
        return error(500, "INTERNAL_ERROR", "The request could not be completed.", List.of());
    }
    private ResponseEntity<ApiError> error(int status, String code, String message, List<FieldError> fields) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code, message, fields));
    }
}
