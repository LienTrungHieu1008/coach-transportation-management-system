package vn.edu.team11.coach.trip.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.application.TripSearchException;
import vn.edu.team11.coach.trip.application.TripSearchModels.ReadData;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;
import vn.edu.team11.coach.trip.application.TripSearchRepository;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.Candidate;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.LegFare;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.StopFact;

@Repository
public class PostgresTripSearchRepository implements TripSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PostgresTripSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public ReadData load(Request request, Instant asOf) {
        try {
            return read(request, asOf);
        } catch (DataAccessException exception) {
            throw TripSearchException.unavailable(exception);
        }
    }

    private ReadData read(Request request, Instant asOf) {
        var parameters = new MapSqlParameterSource()
                .addValue("fromId", request.fromStopId()).addValue("toId", request.toStopId())
                .addValue("stopIds", List.of(request.fromStopId(), request.toStopId()))
                .addValue("asOf", Timestamp.from(asOf))
                .addValue("dayStart", Timestamp.from(request.boardingDate().atStartOfDay(TripSearchPolicy.BOARDING_ZONE).toInstant()))
                .addValue("dayEnd", Timestamp.from(request.boardingDate().plusDays(1).atStartOfDay(TripSearchPolicy.BOARDING_ZONE).toInstant()));
        List<UUID> served = jdbc.query("""
                SELECT s.stop_id FROM stop s
                WHERE s.stop_id IN (:stopIds) AND s.is_active = TRUE
                  AND EXISTS (SELECT 1 FROM route_stop rs JOIN route r ON r.route_id = rs.route_id
                              WHERE rs.stop_id = s.stop_id AND r.is_active = TRUE)
                """, parameters, (rs, row) -> rs.getObject("stop_id", UUID.class));
        boolean fromServed = served.contains(request.fromStopId());
        boolean toServed = served.contains(request.toStopId());
        if (!fromServed || !toServed) return new ReadData(fromServed, toServed, false, List.of());

        Boolean ordered = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM route r
                    JOIN route_stop a ON a.route_id = r.route_id AND a.stop_id = :fromId
                    JOIN route_stop b ON b.route_id = r.route_id AND b.stop_id = :toId
                    WHERE r.is_active = TRUE AND a.stop_order < b.stop_order)
                """, parameters, Boolean.class);
        if (!Boolean.TRUE.equals(ordered)) return new ReadData(true, true, false, List.of());

        // LEFT joins and null-inclusive filters retain candidates that cannot be excluded
        // authoritatively. No paging occurs before full candidate validation.
        List<Candidate> heads = jdbc.query("""
                SELECT t.trip_id, t.route_id, r.route_name, t.status, t.vehicle_id,
                       v.vehicle_type, v.is_active AS vehicle_active, v.seat_layout_id, v.capacity
                FROM trip t JOIN route r ON r.route_id = t.route_id AND r.is_active = TRUE
                JOIN route_stop a ON a.route_id = t.route_id AND a.stop_id = :fromId
                JOIN route_stop b ON b.route_id = t.route_id AND b.stop_id = :toId
                LEFT JOIN trip_stop board ON board.trip_id = t.trip_id AND board.stop_id = :fromId
                LEFT JOIN route_stop first_stop ON first_stop.route_id = t.route_id AND first_stop.stop_order = 0
                LEFT JOIN trip_stop first_time ON first_time.trip_id = t.trip_id AND first_time.stop_id = first_stop.stop_id
                LEFT JOIN vehicle v ON v.vehicle_id = t.vehicle_id
                WHERE t.status = 'Scheduled' AND a.stop_order < b.stop_order
                  AND (board.planned_departure_time IS NULL OR
                       (board.planned_departure_time >= :dayStart AND board.planned_departure_time < :dayEnd))
                  AND (first_time.planned_departure_time IS NULL OR
                       :asOf < first_time.planned_departure_time - INTERVAL '30 minutes')
                """, parameters, (rs, row) -> new Candidate(
                        rs.getObject("trip_id", UUID.class), rs.getObject("route_id", UUID.class),
                        rs.getString("route_name"), rs.getString("status"), rs.getObject("vehicle_id", UUID.class),
                        rs.getString("vehicle_type"), rs.getBoolean("vehicle_active"),
                        rs.getObject("seat_layout_id", UUID.class), rs.getInt("capacity"),
                        List.of(), List.of(), List.of()));
        if (heads.isEmpty()) return new ReadData(true, true, true, List.of());

        parameters.addValue("tripIds", heads.stream().map(Candidate::id).toList());
        Map<UUID, List<StopFact>> stops = new HashMap<>();
        jdbc.query("""
                SELECT t.trip_id, s.stop_id, s.stop_name, rs.stop_order,
                       ts.trip_id AS configured_trip, ts.planned_arrival_time, ts.planned_departure_time
                FROM trip t JOIN route_stop rs ON rs.route_id = t.route_id
                JOIN stop s ON s.stop_id = rs.stop_id
                LEFT JOIN trip_stop ts ON ts.trip_id = t.trip_id AND ts.stop_id = rs.stop_id
                WHERE t.trip_id IN (:tripIds) ORDER BY t.trip_id, rs.stop_order
                """, parameters, (rs, row) -> {
                    stops.computeIfAbsent(rs.getObject("trip_id", UUID.class), ignored -> new ArrayList<>())
                            .add(new StopFact(rs.getObject("stop_id", UUID.class), rs.getString("stop_name"),
                                    rs.getInt("stop_order"), rs.getObject("configured_trip") != null,
                                    instant(rs, "planned_arrival_time"), instant(rs, "planned_departure_time")));
                    return 0;
                });
        Map<UUID, List<LegFare>> fares = new HashMap<>();
        jdbc.query("""
                SELECT trip_id, from_stop_id, to_stop_id, fare_amount_vnd
                FROM leg_fare WHERE trip_id IN (:tripIds)
                """, parameters, (rs, row) -> {
                    fares.computeIfAbsent(rs.getObject("trip_id", UUID.class), ignored -> new ArrayList<>())
                            .add(new LegFare(rs.getObject("from_stop_id", UUID.class),
                                    rs.getObject("to_stop_id", UUID.class), rs.getLong("fare_amount_vnd")));
                    return 0;
                });
        parameters.addValue("vehicleIds", heads.stream().map(Candidate::vehicleId).distinct().toList());
        Map<UUID, List<LayoutSeat>> seats = new HashMap<>();
        jdbc.query("""
                SELECT vehicle_id, physical_seat_id, seat_code, deck, seat_row, seat_column
                FROM physical_seat WHERE vehicle_id IN (:vehicleIds)
                """, parameters, (rs, row) -> {
                    seats.computeIfAbsent(rs.getObject("vehicle_id", UUID.class), ignored -> new ArrayList<>())
                            .add(new LayoutSeat(rs.getObject("physical_seat_id", UUID.class), rs.getString("seat_code"),
                                    rs.getInt("deck"), rs.getInt("seat_row"), rs.getInt("seat_column")));
                    return 0;
                });
        List<Candidate> candidates = heads.stream().map(head -> new Candidate(head.id(), head.routeId(),
                head.routeName(), head.status(), head.vehicleId(), head.vehicleType(), head.vehicleActive(),
                head.layoutId(), head.capacity(), List.copyOf(stops.getOrDefault(head.id(), List.of())),
                List.copyOf(fares.getOrDefault(head.id(), List.of())),
                List.copyOf(seats.getOrDefault(head.vehicleId(), List.of())))).toList();
        return new ReadData(true, true, true, candidates);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
