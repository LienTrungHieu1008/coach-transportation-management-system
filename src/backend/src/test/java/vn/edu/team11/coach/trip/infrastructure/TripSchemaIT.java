
package vn.edu.team11.coach.trip.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
class TripSchemaIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void freshDatabaseHasTripMigration() {
        List<String> versions = jdbc.queryForList(
            """
            SELECT version
            FROM flyway_schema_history
            WHERE success = TRUE
              AND version IS NOT NULL
            ORDER BY installed_rank
            """,
            String.class
        );

        assertThat(versions)
            .containsExactly("1", "2", "3");
    }

    @Test
    void tripTablesExist() {
        List<String> tables = jdbc.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_type = 'BASE TABLE'
            """,
            String.class
        );

        assertThat(tables).contains(
            "route",
            "stop",
            "route_stop",
            "vehicle",
            "physical_seat",
            "schedule",
            "trip",
            "trip_stop",
            "leg_fare"
        );
    }

    @Test
    void freshDatabaseHasNoTripDemoData() {
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM trip",
            Long.class
        )).isZero();

        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM physical_seat",
            Long.class
        )).isZero();
    }

@Test
void rejectsNegativeStopOrder() {
    UUID routeId = UUID.randomUUID();
    UUID stopId = UUID.randomUUID();

    jdbc.update(
        "INSERT INTO route (route_id, route_code, route_name) VALUES (?, ?, ?)",
        routeId, "R-" + routeId, "Test Route"
    );

    jdbc.update(
        "INSERT INTO stop (stop_id, stop_name, address) VALUES (?, ?, ?)",
        stopId, "Test Stop", "Test Address"
    );

    assertThatThrownBy(() -> jdbc.update(
        "INSERT INTO route_stop (route_id, stop_id, stop_order) VALUES (?, ?, ?)",
        routeId, stopId, -1
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void rejectsDuplicateStopOrderWithinRoute() {
    UUID routeId = UUID.randomUUID();
    UUID firstStopId = UUID.randomUUID();
    UUID secondStopId = UUID.randomUUID();

    jdbc.update(
        "INSERT INTO route (route_id, route_code, route_name) VALUES (?, ?, ?)",
        routeId, "R-" + routeId, "Test Route"
    );

    jdbc.update(
        "INSERT INTO stop (stop_id, stop_name, address) VALUES (?, ?, ?)",
        firstStopId, "First Stop", "Address A"
    );

    jdbc.update(
        "INSERT INTO stop (stop_id, stop_name, address) VALUES (?, ?, ?)",
        secondStopId, "Second Stop", "Address B"
    );

    jdbc.update(
        "INSERT INTO route_stop (route_id, stop_id, stop_order) VALUES (?, ?, ?)",
        routeId, firstStopId, 0
    );

    assertThatThrownBy(() -> jdbc.update(
        "INSERT INTO route_stop (route_id, stop_id, stop_order) VALUES (?, ?, ?)",
        routeId, secondStopId, 0
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void rejectsNegativeSeatCoordinates() {
    UUID vehicleId = UUID.randomUUID();
    UUID seatId = UUID.randomUUID();

    jdbc.update(
        """
        INSERT INTO vehicle
            (vehicle_id, vehicle_code, vehicle_type, capacity)
        VALUES (?, ?, ?, ?)
        """,
        vehicleId, "V-" + vehicleId, "SLEEPER", 6
    );

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO physical_seat
            (physical_seat_id, vehicle_id, seat_code,
             deck, seat_row, seat_column)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        seatId, vehicleId, "A1", -1, 0, 0
    )).isInstanceOf(DataIntegrityViolationException.class);
}


@Test
@Sql("/fixtures/trip_demo.sql")
void demoFixtureContainsExpectedTripData() {
    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM route", Long.class
    )).isEqualTo(1L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM stop", Long.class
    )).isEqualTo(3L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM route_stop", Long.class
    )).isEqualTo(3L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM vehicle", Long.class
    )).isEqualTo(1L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM physical_seat", Long.class
    )).isEqualTo(6L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM schedule", Long.class
    )).isEqualTo(2L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM trip", Long.class
    )).isEqualTo(2L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM trip_stop", Long.class
    )).isEqualTo(6L);

    assertThat(jdbc.queryForObject(
        "SELECT count(*) FROM leg_fare", Long.class
    )).isEqualTo(4L);
}


@Test
@Sql("/fixtures/trip_demo.sql")
void demoRouteHasZeroBasedStopOrder() {
    List<Integer> stopOrders = jdbc.queryForList(
        """
        SELECT stop_order
        FROM route_stop
        WHERE route_id = ?
        ORDER BY stop_order
        """,
        Integer.class,
        UUID.fromString("44444444-4444-4444-8444-444444444444")
    );

    assertThat(stopOrders).containsExactly(0, 1, 2);
}

@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveCorrectSegmentFare() {
    List<UUID> tripIds = List.of(
        UUID.fromString("11111111-1111-4111-8111-111111111111"),
        UUID.fromString("11111111-1111-4111-8111-111111111112")
    );

    for (UUID tripId : tripIds) {
        Long totalFare = jdbc.queryForObject(
            """
            SELECT SUM(fare_amount_vnd)
            FROM leg_fare
            WHERE trip_id = ?
            """,
            Long.class,
            tripId
        );

        assertThat(totalFare).isEqualTo(250000L);
    }
}


@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveCorrectUtcDepartureTimes() {
    UUID stopA = UUID.fromString(
        "22222222-2222-4222-8222-222222222222"
    );

    UUID dayTrip = UUID.fromString(
        "11111111-1111-4111-8111-111111111111"
    );

    UUID overnightTrip = UUID.fromString(
        "11111111-1111-4111-8111-111111111112"
    );

    Instant dayDeparture = jdbc.queryForObject(
        """
        SELECT planned_departure_time
        FROM trip_stop
        WHERE trip_id = ? AND stop_id = ?
        """,
        (rs, rowNum) -> rs.getObject(
            "planned_departure_time",
            OffsetDateTime.class
        ).toInstant(),
        dayTrip,
        stopA
    );

    Instant overnightDeparture = jdbc.queryForObject(
        """
        SELECT planned_departure_time
        FROM trip_stop
        WHERE trip_id = ? AND stop_id = ?
        """,
        (rs, rowNum) -> rs.getObject(
            "planned_departure_time",
            OffsetDateTime.class
        ).toInstant(),
        overnightTrip,
        stopA
    );

    assertThat(dayDeparture)
        .isEqualTo(Instant.parse("2026-11-20T02:00:00Z"));

    assertThat(overnightDeparture)
        .isEqualTo(Instant.parse("2026-11-20T15:30:00Z"));
}


@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveCorrectBookingCutoff() {
    UUID stopA = UUID.fromString(
        "22222222-2222-4222-8222-222222222222"
    );

    UUID[] tripIds = {
        UUID.fromString("11111111-1111-4111-8111-111111111111"),
        UUID.fromString("11111111-1111-4111-8111-111111111112")
    };

    Instant[] expectedCutoffs = {
        Instant.parse("2026-11-20T01:30:00Z"),
        Instant.parse("2026-11-20T15:00:00Z")
    };

    for (int i = 0; i < tripIds.length; i++) {
        Instant departure = jdbc.queryForObject(
            """
            SELECT planned_departure_time
            FROM trip_stop
            WHERE trip_id = ? AND stop_id = ?
            """,
            (rs, rowNum) -> rs.getObject(
                "planned_departure_time",
                OffsetDateTime.class
            ).toInstant(),
            tripIds[i],
            stopA
        );

        Instant bookingClosesAt = departure.minusSeconds(30 * 60);

        assertThat(bookingClosesAt)
            .isEqualTo(expectedCutoffs[i]);
    }
}


@Test
@Sql("/fixtures/trip_demo.sql")
void demoVehicleHasStableSixSeatLayout() {
    UUID vehicleId = UUID.fromString(
        "66666666-6666-4666-8666-666666666666"
    );

    List<String> seats = jdbc.queryForList(
        """
        SELECT physical_seat_id::text || '|' ||
               seat_code || '|' ||
               deck || '|' ||
               seat_row || '|' ||
               seat_column
        FROM physical_seat
        WHERE vehicle_id = ?
        ORDER BY seat_row, seat_column
        """,
        String.class,
        vehicleId
    );

    assertThat(seats).containsExactly(
        "77777777-7777-4777-8777-777777777777|A1|0|0|0",
        "88888888-8888-4888-8888-888888888888|A2|0|0|2",
        "77777777-7777-4777-8777-777777777778|B1|0|1|0",
        "77777777-7777-4777-8777-777777777779|B2|0|1|2",
        "77777777-7777-4777-8777-777777777780|C1|0|2|0",
        "77777777-7777-4777-8777-777777777781|C2|0|2|2"
    );
}


@Test
void rejectsDuplicatePhysicalSeatPosition() {
    UUID vehicleId = UUID.randomUUID();

    jdbc.update(
        """
        INSERT INTO vehicle
            (vehicle_id, vehicle_code, vehicle_type, capacity)
        VALUES (?, ?, ?, ?)
        """,
        vehicleId, "V-" + vehicleId, "DEMO_COACH", 6
    );

    jdbc.update(
        """
        INSERT INTO physical_seat
            (physical_seat_id, vehicle_id, seat_code,
             deck, seat_row, seat_column)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(), vehicleId, "A1", 0, 0, 0
    );

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO physical_seat
            (physical_seat_id, vehicle_id, seat_code,
             deck, seat_row, seat_column)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(), vehicleId, "A2", 0, 0, 0
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
@Sql("/fixtures/trip_demo.sql")
void rejectsTripStopOutsideItsRoute() {
    UUID otherStopId = UUID.randomUUID();

    jdbc.update(
        """
        INSERT INTO stop (stop_id, stop_name, address)
        VALUES (?, ?, ?)
        """,
        otherStopId, "Unserved Stop", "Test Address"
    );

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO trip_stop (
            trip_id,
            route_id,
            stop_id,
            planned_departure_time
        )
        VALUES (?, ?, ?, ?)
        """,
        UUID.fromString("11111111-1111-4111-8111-111111111111"),
        UUID.fromString("44444444-4444-4444-8444-444444444444"),
        otherStopId,
        java.time.OffsetDateTime.parse("2026-11-20T03:00:00Z")
    )).isInstanceOf(DataIntegrityViolationException.class);
}

}
