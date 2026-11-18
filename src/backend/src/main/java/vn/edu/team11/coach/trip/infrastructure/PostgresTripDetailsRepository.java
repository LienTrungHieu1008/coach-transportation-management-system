package vn.edu.team11.coach.trip.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import vn.edu.team11.coach.trip.application.TripDetailsRepository;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.*;

@Repository
public class PostgresTripDetailsRepository implements TripDetailsRepository {
    private final JdbcTemplate jdbc;
    public PostgresTripDetailsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public Optional<Facts> load(UUID tripId) {
        try {
            List<Facts> heads = jdbc.query("""
                    SELECT t.trip_id, t.status, r.route_id, r.route_name, r.is_active AS route_active,
                           v.vehicle_id, v.vehicle_type, v.seat_layout_id, v.is_active AS vehicle_active, v.capacity
                    FROM trip t LEFT JOIN route r ON r.route_id = t.route_id
                    LEFT JOIN vehicle v ON v.vehicle_id = t.vehicle_id WHERE t.trip_id = ?
                    """, (rs, row) -> new Facts(tripId, TripStatus.valueOf(rs.getString("status")),
                            rs.getObject("route_id") == null ? null : new Reference(rs.getObject("route_id", UUID.class), rs.getString("route_name")),
                            rs.getBoolean("route_active"), rs.getObject("vehicle_id") == null ? null : new Vehicle(
                                    rs.getObject("vehicle_id", UUID.class), rs.getString("vehicle_type"), rs.getObject("seat_layout_id", UUID.class)),
                            rs.getBoolean("vehicle_active"), rs.getInt("capacity"), List.of(), List.of(), List.of()), tripId);
            if (heads.isEmpty()) return Optional.empty();
            Facts head = heads.getFirst();
            var stops = jdbc.query("""
                    SELECT s.stop_id, s.stop_name, s.is_active, rs.stop_order, ts.trip_id AS configured,
                           ts.planned_arrival_time, ts.planned_departure_time
                    FROM trip t JOIN route_stop rs ON rs.route_id = t.route_id
                    LEFT JOIN stop s ON s.stop_id = rs.stop_id
                    LEFT JOIN trip_stop ts ON ts.trip_id = t.trip_id AND ts.route_id = rs.route_id AND ts.stop_id = rs.stop_id
                    WHERE t.trip_id = ? ORDER BY rs.stop_order
                    """, (rs, row) -> new StopFact(rs.getObject("stop_id", UUID.class), rs.getString("stop_name"),
                            rs.getInt("stop_order"), rs.getBoolean("is_active"), rs.getObject("configured") != null,
                            instant(rs, "planned_arrival_time"), instant(rs, "planned_departure_time")), tripId);
            var fares = jdbc.query("SELECT from_stop_id, to_stop_id, fare_amount_vnd FROM leg_fare WHERE trip_id = ?",
                    (rs, row) -> new Fare(rs.getObject("from_stop_id", UUID.class), rs.getObject("to_stop_id", UUID.class), rs.getLong("fare_amount_vnd")), tripId);
            List<LayoutSeat> seats = head.vehicle() == null ? List.of() : jdbc.query("""
                    SELECT physical_seat_id, seat_code, deck, seat_row, seat_column
                    FROM physical_seat WHERE vehicle_id = ? ORDER BY deck, seat_row, seat_column, physical_seat_id
                    """, (rs, row) -> new LayoutSeat(rs.getObject("physical_seat_id", UUID.class), rs.getString("seat_code"),
                            rs.getInt("deck"), rs.getInt("seat_row"), rs.getInt("seat_column")), head.vehicle().id());
            return Optional.of(new Facts(head.tripId(), head.status(), head.route(), head.routeActive(), head.vehicle(),
                    head.vehicleActive(), head.capacity(), stops, fares, seats));
        } catch (DataAccessException failure) {
            throw TripDetailsException.unavailable(failure);
        }
    }
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public List<UUID> existingStops(List<UUID> stopIds) {
        try {
            return new NamedParameterJdbcTemplate(jdbc).query("SELECT stop_id FROM stop WHERE stop_id IN (:ids)",
                    java.util.Map.of("ids", stopIds), (rs, row) -> rs.getObject("stop_id", UUID.class));
        } catch (DataAccessException failure) {
            throw TripDetailsException.unavailable(failure);
        }
    }
}
