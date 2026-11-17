package vn.edu.team11.coach.trip.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.team11.coach.trip.application.StopCatalogueService;
import vn.edu.team11.coach.trip.application.StopCatalogueUnavailableException;

@RestController
public class StopController {
    private final StopCatalogueService service;

    public StopController(StopCatalogueService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/stops")
    public ResponseEntity<StopCatalogueService.Catalogue> listStops() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listStops());
    }

    // Endpoint-local mapping only; shared API/security error handling belongs to C2.
    @ExceptionHandler(StopCatalogueUnavailableException.class)
    ResponseEntity<Map<String, Object>> unavailable() {
        return error(503, "TRIP_DATA_UNAVAILABLE",
                "Trip information could not be verified. Please retry.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> internalError() {
        return error(500, "INTERNAL_ERROR", "The request could not be completed.");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String code, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(
                Map.of("code", code, "message", message, "fieldErrors", List.of()));
    }
}
