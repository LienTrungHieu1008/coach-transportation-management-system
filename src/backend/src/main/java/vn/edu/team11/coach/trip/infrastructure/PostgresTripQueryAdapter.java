
package vn.edu.team11.coach.trip.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.SeatLayout;
import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripSeatFacts;
import vn.edu.team11.coach.trip.contract.TripStatus;

@Repository
public class PostgresTripQueryAdapter implements TripQueryPort {

    private final JdbcTemplate jdbc;

    public PostgresTripQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    @Transactional(
            readOnly = true,
            propagation = Propagation.MANDATORY
    )
    public TripSeatFacts getSeatAvailabilityFacts(
            UUID tripId,
            UUID fromStopId,
            UUID toStopId,
            Instant asOf
    ) {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(fromStopId, "fromStopId");
        Objects.requireNonNull(toStopId, "toStopId");
        Objects.requireNonNull(asOf, "asOf");
        TripRow trip;
        try {
            trip = jdbc.queryForObject(
                    """
                    SELECT trip_id, route_id, vehicle_id, status
                    FROM trip
                    WHERE trip_id = ?
                    """,
                    (rs, rowNum) -> new TripRow(
                            rs.getObject("trip_id", UUID.class),
                            rs.getObject("route_id", UUID.class),
                            rs.getObject("vehicle_id", UUID.class),
                            rs.getString("status")
                    ),
                    tripId
            );

        } catch (EmptyResultDataAccessException ex) {
            throw new TripQueryException(
                    TripQueryErrorCode.TRIP_NOT_FOUND,
                    "Trip not found"
            );

        } catch (DataAccessException ex) {
            throw new TripQueryException(
                    TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                    "Unable to retrieve Trip data",
                    ex
            );
        }


        List<TripStopRow> tripStops;
        try {
        tripStops = jdbc.query(
                """
                SELECT
                        ts.stop_id,
                        rs.stop_order,
                        ts.planned_arrival_time,
                        ts.planned_departure_time
                FROM trip_stop ts
                JOIN route_stop rs
                ON rs.route_id = ts.route_id
                AND rs.stop_id = ts.stop_id
                WHERE ts.trip_id = ?
                AND ts.route_id = ?
                ORDER BY rs.stop_order
                """,
                (rs, rowNum) -> new TripStopRow(
                        rs.getObject("stop_id", UUID.class),
                        rs.getInt("stop_order"),
                        rs.getObject(
                                "planned_arrival_time",
                                OffsetDateTime.class
                        ) == null
                                ? null
                                : rs.getObject(
                                        "planned_arrival_time",
                                        OffsetDateTime.class
                                ).toInstant(),
                        rs.getObject(
                                "planned_departure_time",
                                OffsetDateTime.class
                        ) == null
                                ? null
                                : rs.getObject(
                                        "planned_departure_time",
                                        OffsetDateTime.class
                                ).toInstant()
                ),
                tripId,
                trip.routeId()
        );
        } catch (DataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                "Unable to retrieve Trip stops",
                ex
        );
        }

        Long routeStopCount;

        try {
        routeStopCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM route_stop
                WHERE route_id = ?
                """,
                Long.class,
                trip.routeId()
        );
        } catch (DataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                "Unable to verify Route stops",
                ex
        );
        }

        if (routeStopCount == null
                || routeStopCount < 2
                || routeStopCount != tripStops.size()) {
        throw new TripQueryException(
                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                "Trip does not contain all required Route stops"
        );
        }

        if (tripStops.size() < 2) {
        throw new TripQueryException(
                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                "Trip must contain at least two stops"
        );
        }

        for (int i = 0; i < tripStops.size(); i++) {
                TripStopRow stop = tripStops.get(i);
                if (stop.stopOrder() != i) {
                        throw new TripQueryException(
                                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                                "Trip stop order is incomplete or invalid"
                        );
                }
        }

        boolean fromServed;
        boolean toServed;

        try {
        fromServed = Boolean.TRUE.equals(jdbc.queryForObject(
                """
                SELECT EXISTS (
                        SELECT 1
                        FROM stop
                        WHERE stop_id = ?
                        AND is_active = TRUE
                )
                """,
                Boolean.class,
                fromStopId
        ));

        toServed = Boolean.TRUE.equals(jdbc.queryForObject(
                """
                SELECT EXISTS (
                        SELECT 1
                        FROM stop
                        WHERE stop_id = ?
                        AND is_active = TRUE
                )
                """,
                Boolean.class,
                toStopId
        ));
        } catch (DataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                "Unable to verify Stops",
                ex
        );
        }

        if (!fromServed || !toServed) {
        throw new TripQueryException(
                TripQueryErrorCode.STOP_NOT_SERVED,
                "One or both Stops are not served"
        );
        }

        Optional<TripStopRow> fromStop = tripStops.stream()
                .filter(stop -> stop.stopId().equals(fromStopId))
                .findFirst();

        Optional<TripStopRow> toStop = tripStops.stream()
                .filter(stop -> stop.stopId().equals(toStopId))
                .findFirst();

        if (fromStop.isEmpty() || toStop.isEmpty()
                || fromStop.get().stopOrder() >= toStop.get().stopOrder()) {
        throw new TripQueryException(
                TripQueryErrorCode.INVALID_SEGMENT,
                "Invalid Trip segment"
        );
        }


        for (int i = 0; i < tripStops.size(); i++) {
        TripStopRow current = tripStops.get(i);

        if (i == 0 && current.departureTime() == null) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "First Trip stop must have a departure time"
                );
        }

        if (i == tripStops.size() - 1
                && current.arrivalTime() == null) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "Last Trip stop must have an arrival time"
                );
        }

        if (i > 0) {
                TripStopRow previous = tripStops.get(i - 1);

                Instant previousTime = previous.departureTime() != null
                        ? previous.departureTime()
                        : previous.arrivalTime();

                Instant currentTime = current.arrivalTime() != null
                        ? current.arrivalTime()
                        : current.departureTime();

                if (previousTime == null || currentTime == null
                        || !currentTime.isAfter(previousTime)) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "Trip stop times are missing or not chronological"
                );
                }
        }
        }

        Instant bookingClosesAt = tripStops.get(0)
                .departureTime()
                .minusSeconds(30 * 60);

        TripStatus status;

        try {
        status = TripStatus.valueOf(trip.status());
        } catch (IllegalArgumentException | NullPointerException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                "Trip has an invalid status",
                ex
        );
        }

        if (status != TripStatus.Scheduled
                || !asOf.isBefore(bookingClosesAt)) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_NOT_BOOKABLE,
                "Trip is not bookable"
        );
        }

        SeatLayout seatLayout;

        try {
        VehicleRow vehicle = jdbc.queryForObject(
                """
                SELECT seat_layout_id, capacity
                FROM vehicle
                WHERE vehicle_id = ?
                AND is_active = TRUE
                """,
                (rs, rowNum) -> new VehicleRow(
                        rs.getObject("seat_layout_id", UUID.class),
                        rs.getInt("capacity")
                ),
                trip.vehicleId()
        );

        if (vehicle == null) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "Vehicle configuration is missing"
                );
        }

        List<LayoutSeat> seats = jdbc.query(
                """
                SELECT
                        physical_seat_id,
                        seat_code,
                        deck,
                        seat_row,
                        seat_column
                FROM physical_seat
                WHERE vehicle_id = ?
                ORDER BY deck, seat_row, seat_column
                """,
                (rs, rowNum) -> new LayoutSeat(
                        rs.getObject("physical_seat_id", UUID.class),
                        rs.getString("seat_code"),
                        rs.getInt("deck"),
                        rs.getInt("seat_row"),
                        rs.getInt("seat_column")
                ),
                trip.vehicleId()
        );

        if (seats.size() != vehicle.capacity()) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "Vehicle seat count does not match capacity"
                );
        }

        seatLayout = new SeatLayout(
                vehicle.seatLayoutId(),
                seats
        );

        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                "Vehicle configuration is missing",
                ex
        );

        } catch (DataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                "Unable to retrieve Vehicle seat layout",
                ex
        );

        } catch (IllegalArgumentException | NullPointerException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                "Vehicle seat layout is invalid",
                ex
        );
        }


        try {
        // Essential data is a property of the whole Trip, not only the requested Segment.
        for (int i = 0; i < tripStops.size() - 1; i++) {
                UUID legFrom = tripStops.get(i).stopId();
                UUID legTo = tripStops.get(i + 1).stopId();

                Long fare = jdbc.query(
                        """
                        SELECT fare_amount_vnd
                        FROM leg_fare
                        WHERE trip_id = ?
                        AND from_stop_id = ?
                        AND to_stop_id = ?
                        """,
                        rs -> rs.next()
                                ? rs.getLong("fare_amount_vnd")
                                : null,
                        tripId,
                        legFrom,
                        legTo
                );

                if (fare == null || fare < 0) {
                throw new TripQueryException(
                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE,
                        "Trip leg fare is missing or invalid"
                );
                }
        }

        } catch (DataAccessException ex) {
        throw new TripQueryException(
                TripQueryErrorCode.TRIP_DATA_UNAVAILABLE,
                "Unable to retrieve Trip leg fares",
                ex
        );
        }
        return new TripSeatFacts(
                trip.tripId(),
                fromStopId,
                toStopId,
                fromStop.get().stopOrder(),
                toStop.get().stopOrder(),
                status,
                bookingClosesAt,
                seatLayout,
                asOf
        );

    }

        private record TripRow(
                UUID tripId,
                UUID routeId,
                UUID vehicleId,
                String status
        ) {
        }

        private record TripStopRow(
                UUID stopId,
                int stopOrder,
                Instant arrivalTime,
                Instant departureTime
        ) {
        }

        private record VehicleRow(
                UUID seatLayoutId,
                int capacity
        ) {
        }

}
