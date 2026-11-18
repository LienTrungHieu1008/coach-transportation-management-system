package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.team11.coach.trip.application.TripDetailsModels.Request;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Result;

@Service
public class TripDetailsService {
    private final TripDetailsRepository repository;
    private final Clock clock;
    private final TripDetailsPolicy policy = new TripDetailsPolicy();

    public TripDetailsService(TripDetailsRepository repository, @Qualifier("tripDetailsClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Result details(Request request) {
        Instant asOf = clock.instant();
        var facts = repository.load(request.tripId()).orElseThrow(TripDetailsException::notFound);
        try {
            return policy.evaluate(facts, request.fromStopId(), request.toStopId(), asOf);
        } catch (TripDetailsPolicy.InvalidSegmentException failure) {
            if (!request.fromStopId().equals(request.toStopId())) {
                var existing = repository.existingStops(List.of(request.fromStopId(), request.toStopId()));
                if (!existing.contains(request.fromStopId())) throw TripDetailsException.stopNotServed("fromStopId");
                if (!existing.contains(request.toStopId())) throw TripDetailsException.stopNotServed("toStopId");
            }
            throw TripDetailsException.segment();
        }
    }
}
