package vn.edu.team11.coach.trip.application;

import java.util.List;

public interface StopCatalogueRepository {
    List<ServedStop> findServedStops();
}
