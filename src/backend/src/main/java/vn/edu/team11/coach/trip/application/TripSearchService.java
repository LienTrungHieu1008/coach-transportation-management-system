package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.application.TripSearchModels.Item;
import vn.edu.team11.coach.trip.application.TripSearchModels.Page;
import vn.edu.team11.coach.trip.application.TripSearchModels.Reference;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy;

@Service
public class TripSearchService {
    private final TripSearchRepository repository;
    private final Clock clock;
    private final TripSearchPolicy policy = new TripSearchPolicy();

    public TripSearchService(TripSearchRepository repository, @Qualifier("tripSearchClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page search(Request request) {
        Instant asOf = clock.instant();
        if (request.boardingDate().isBefore(asOf.atZone(TripSearchPolicy.BOARDING_ZONE).toLocalDate())) {
            throw TripSearchException.criteria("PAST_BOARDING_DATE", "boardingDate", "Select today or a later boarding date.");
        }
        var data = repository.load(request, asOf);
        if (!data.fromServed() || !data.toServed()) {
            throw TripSearchException.criteria("STOP_NOT_SERVED",
                    !data.fromServed() ? "fromStopId" : "toStopId", "Select a served Stop.");
        }
        if (request.fromStopId().equals(request.toStopId()) || !data.orderedRouteExists()) {
            throw TripSearchException.criteria("INVALID_SEGMENT", "toStopId", "Boarding Stop must precede alighting Stop.");
        }
        var matches = new ArrayList<Item>();
        try {
            for (var candidate : data.candidates()) {
                policy.evaluate(candidate, request.fromStopId(), request.toStopId(), request.boardingDate(), asOf)
                        .ifPresent(verified -> matches.add(new Item(candidate.id(),
                                new Reference(candidate.routeId(), candidate.routeName()),
                                new Reference(verified.from().id(), verified.from().name()),
                                new Reference(verified.to().id(), verified.to().name()),
                                verified.from().departure(), verified.to().arrival(), candidate.vehicleType(),
                                verified.fare(), verified.cutoff())));
            }
        } catch (TripSearchPolicy.IncompleteTripException exception) {
            throw TripSearchException.unavailable(exception);
        }
        // UUID canonical text order matches PostgreSQL UUID byte order, unlike UUID.compareTo.
        matches.sort(Comparator.comparing(Item::boardingTime).thenComparing(item -> item.tripId().toString()));
        long total = matches.size();
        long pages = total / request.size() + (total % request.size() == 0 ? 0 : 1);
        long offset = (long) request.page() * request.size();
        var items = matches.stream().skip(offset).limit(request.size()).toList();
        return new Page(items, request.page(), request.size(), total, pages, asOf);
    }
}
