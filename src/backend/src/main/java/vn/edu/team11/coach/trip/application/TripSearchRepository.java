package vn.edu.team11.coach.trip.application;

import java.time.Instant;

import vn.edu.team11.coach.trip.application.TripSearchModels.ReadData;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;

public interface TripSearchRepository {
    ReadData load(Request request, Instant asOf);
}
