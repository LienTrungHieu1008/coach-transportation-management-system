package vn.edu.team11.coach.trip.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.team11.coach.trip.application.TripSearchException;
import vn.edu.team11.coach.trip.application.TripSearchModels.Page;
import vn.edu.team11.coach.trip.application.TripSearchService;

@RestController
public class TripSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(TripSearchController.class);
    private final TripSearchService service;
    private final TripSearchRequestParser parser;

    public TripSearchController(TripSearchService service, TripSearchRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @GetMapping("/api/v1/trips")
    public ResponseEntity<Page> search(@RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.search(parser.parse(parameters)));
    }

    // Scoped to this endpoint; C2 common/security mapping remains untouched.
    @ExceptionHandler(TripSearchException.class)
    ResponseEntity<Map<String, Object>> searchError(TripSearchException exception) {
        if (exception.status() == 503) LOG.error("Trip search data unavailable", exception);
        return error(exception.status(), exception.code(), exception.getMessage(), exception.fieldErrors());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> internalError(Exception exception) {
        LOG.error("Unexpected Trip search failure", exception);
        return error(500, "INTERNAL_ERROR", "The request could not be completed.", List.of());
    }

    private ResponseEntity<Map<String, Object>> error(int status, String code, String message, List<?> fields) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(
                Map.of("code", code, "message", message, "fieldErrors", fields));
    }
}
