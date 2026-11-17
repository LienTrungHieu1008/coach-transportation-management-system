package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StopCatalogueService {
    private final StopCatalogueRepository repository;
    private final Clock clock;

    public StopCatalogueService(StopCatalogueRepository repository,
                               @Qualifier("stopCatalogueClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Catalogue listStops() {
        Instant asOf = clock.instant();
        return new Catalogue(repository.findServedStops(), asOf);
    }

    public record Catalogue(List<ServedStop> items, Instant serverTime) {
        public Catalogue {
            items = List.copyOf(items);
        }
    }
}
