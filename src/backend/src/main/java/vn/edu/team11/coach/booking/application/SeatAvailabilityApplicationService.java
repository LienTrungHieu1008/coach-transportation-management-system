package vn.edu.team11.coach.booking.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.booking.api.SeatAvailabilityResponse;
import vn.edu.team11.coach.booking.domain.SeatAllocationService;
import vn.edu.team11.coach.booking.domain.Segment;
import vn.edu.team11.coach.booking.infrastructure.BookingRepository;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripStatus;

@Service
public class SeatAvailabilityApplicationService {
    private final ObjectProvider<TripQueryPort> tripQueries;
    private final BookingRepository repository;
    private final Clock clock;

    public SeatAvailabilityApplicationService(ObjectProvider<TripQueryPort> tripQueries, BookingRepository repository,
                                              @Qualifier("bookingClock") Clock clock) {
        this.tripQueries = tripQueries;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public SeatAvailabilityResponse getSeatAvailability(UUID tripId, UUID fromStopId, UUID toStopId) {
        var asOf = clock.instant().truncatedTo(ChronoUnit.MICROS);
        try {
            var port = tripQueries.getIfAvailable();
            if (port == null) {
                throw new TripQueryException(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE, "Trip data unavailable");
            }
            var facts = port.getSeatAvailabilityFacts(tripId, fromStopId, toStopId, asOf);
            if (facts == null || !tripId.equals(facts.tripId()) || !fromStopId.equals(facts.fromStopId())
                    || !toStopId.equals(facts.toStopId()) || !asOf.equals(facts.asOf())
                    || facts.status() != TripStatus.Scheduled || !asOf.isBefore(facts.bookingClosesAt())) {
                throw new TripQueryException(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE, "Unverified trip data");
            }
            var segment = new Segment(facts.fromStopIndex(), facts.toStopIndex());
            var seatIds = facts.seatLayout().seats().stream().map(LayoutSeat::physicalSeatId).toList();
            var knownSeats = seatIds.stream().collect(Collectors.toSet());
            var candidates = repository.findCandidates(tripId, segment);
            if (candidates == null || candidates.stream().anyMatch(row -> row == null
                    || !tripId.equals(row.tripId()) || !knownSeats.contains(row.physicalSeatId())
                    || !segment.overlaps(row.segment()))) {
                throw new TripQueryException(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE, "Unverified allocation data");
            }
            var seats = new SeatAllocationService().getAvailability(tripId, segment, seatIds, candidates, asOf).stream()
                    .map(seat -> new SeatAvailabilityResponse.SeatState(seat.physicalSeatId(),
                            seat.status().name(), seat.expiresAt())).toList();
            return new SeatAvailabilityResponse(tripId, fromStopId, toStopId, asOf, facts.bookingClosesAt(),
                    facts.seatLayout(), seats);
        } catch (DataAccessException | TransactionException failure) {
            throw new TripQueryException(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE, "Availability data unavailable", failure);
        }
    }
}
