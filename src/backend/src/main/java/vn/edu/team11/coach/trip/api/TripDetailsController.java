package vn.edu.team11.coach.trip.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.team11.coach.trip.application.TripDetailsService;

@RestController
public class TripDetailsController {
    private final TripDetailsService service;
    private final TripDetailsRequestParser parser;
    public TripDetailsController(TripDetailsService service, TripDetailsRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }
    @GetMapping("/api/v1/trips/{tripId}")
    public ResponseEntity<TripDetailsResponse> details(@PathVariable String tripId,
                                                     @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(TripDetailsResponse.from(service.details(parser.parse(tripId, parameters))));
    }
}
