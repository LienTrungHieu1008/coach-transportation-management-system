package vn.edu.team11.coach.booking.api;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.edu.team11.coach.trip.contract.TripQueryException;

@RestControllerAdvice(assignableTypes = SeatAvailabilityController.class)
public class SeatAvailabilityExceptionHandler {
    @ExceptionHandler(SeatAvailabilityController.InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(SeatAvailabilityController.InvalidRequestException failure) {
        return error(400, "INVALID_REQUEST", "Tham số UUID phải có một giá trị đúng định dạng chuẩn",
                List.of(new FieldError(failure.field(), "Cung cấp một UUID 36 ký tự có dấu gạch nối đúng vị trí")));
    }

    @ExceptionHandler(TripQueryException.class)
    public ResponseEntity<ApiError> tripFailure(TripQueryException failure) {
        return switch (failure.getCode()) {
            case TRIP_NOT_FOUND -> error(404, "TRIP_NOT_FOUND", "Không tìm thấy chuyến xe", List.of());
            case STOP_NOT_SERVED -> error(422, "STOP_NOT_SERVED", "Chuyến xe không phục vụ điểm đã chọn", List.of());
            case INVALID_SEGMENT -> error(422, "INVALID_SEGMENT", "Điểm lên phải đứng trước điểm xuống", List.of());
            case TRIP_NOT_BOOKABLE -> error(409, "TRIP_NOT_BOOKABLE", "Chuyến xe đã đóng bán", List.of());
            case ESSENTIAL_DATA_INCOMPLETE -> error(409, "ESSENTIAL_DATA_INCOMPLETE", "Chuyến xe thiếu dữ liệu bắt buộc", List.of());
            case TRIP_DATA_UNAVAILABLE -> unavailable();
        };
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ApiError> unavailable() {
        return error(503, "AVAILABILITY_UNAVAILABLE", "Chưa thể xác minh tình trạng ghế", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internalFailure() {
        return error(500, "INTERNAL_ERROR", "Không thể xử lý yêu cầu", List.of());
    }

    private static ResponseEntity<ApiError> error(int status, String code, String message, List<FieldError> fields) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code, message, fields));
    }

    public record ApiError(String code, String message, List<FieldError> fieldErrors) {
    }

    public record FieldError(String field, String message) {
    }
}
