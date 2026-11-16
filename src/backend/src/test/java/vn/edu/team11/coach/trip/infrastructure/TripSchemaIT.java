
package vn.edu.team11.coach.trip.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
        INSERT INTO vehicle (
            vehicle_id,
            vehicle_code,
            license_plate,
            vehicle_type,
            capacity,
            layout_template,
            seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "PLATE-" + vehicleId,
        "SLEEPER",
        6,
        "SLEEPER_6_1_DECK",
        UUID.randomUUID()
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
        INSERT INTO vehicle (
            vehicle_id,
            vehicle_code,
            license_plate,
            vehicle_type,
            capacity,
            layout_template,
            seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "PLATE-" + vehicleId,
        "DEMO_COACH",
        6,
        "SEATED_6_1_DECK",
        UUID.randomUUID()
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


@Test
@Sql("/fixtures/trip_demo.sql")
void demoVehicleHasStableIdentityAndLayout() {
    UUID vehicleId = UUID.fromString(
        "66666666-6666-4666-8666-666666666666"
    );

    Map<String, Object> vehicle = jdbc.queryForMap(
        """
        SELECT license_plate, layout_template, seat_layout_id
        FROM vehicle
        WHERE vehicle_id = ?
        """,
        vehicleId
    );

    assertThat(vehicle.get("license_plate"))
        .isEqualTo("47B-123.45");

    assertThat(vehicle.get("layout_template"))
        .isEqualTo("SEATED_6_1_DECK");

    assertThat(vehicle.get("seat_layout_id"))
        .isEqualTo(UUID.fromString(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
        ));
}

@Test
@Sql("/fixtures/trip_demo.sql")
void rejectsDuplicateVehicleLicensePlate() {
    UUID vehicleId = UUID.randomUUID();

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO vehicle (
            vehicle_id, vehicle_code, license_plate,
            vehicle_type, capacity, layout_template, seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "47B-123.45",
        "DEMO_COACH",
        6,
        "SEATED_6_1_DECK",
        UUID.randomUUID()
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
@Sql("/fixtures/trip_demo.sql")
void rejectsDuplicateVehicleSeatLayoutId() {
    UUID vehicleId = UUID.randomUUID();

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO vehicle (
            vehicle_id, vehicle_code, license_plate,
            vehicle_type, capacity, layout_template, seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "PLATE-" + vehicleId,
        "DEMO_COACH",
        6,
        "SEATED_6_1_DECK",
        UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void rejectsBlankVehicleLicensePlate() {
    UUID vehicleId = UUID.randomUUID();

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO vehicle (
            vehicle_id, vehicle_code, license_plate,
            vehicle_type, capacity, layout_template, seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "   ",
        "DEMO_COACH",
        6,
        "SEATED_6_1_DECK",
        UUID.randomUUID()
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void rejectsBlankVehicleLayoutTemplate() {
    UUID vehicleId = UUID.randomUUID();

    assertThatThrownBy(() -> jdbc.update(
        """
        INSERT INTO vehicle (
            vehicle_id, vehicle_code, license_plate,
            vehicle_type, capacity, layout_template, seat_layout_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        vehicleId,
        "V-" + vehicleId,
        "PLATE-" + vehicleId,
        "DEMO_COACH",
        6,
        "   ",
        UUID.randomUUID()
    )).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
@Sql("/fixtures/trip_demo.sql")
void demoRouteStopIndicesAreContiguousFromZero() {
    Long invalidCount = jdbc.queryForObject(
        """
        SELECT count(*)
        FROM (
            SELECT
                route_id,
                stop_order,
                row_number() OVER (
                    PARTITION BY route_id
                    ORDER BY stop_order
                ) - 1 AS expected_order
            FROM route_stop
        ) ordered_stops
        WHERE stop_order <> expected_order
        """,
        Long.class
    );

    assertThat(invalidCount).isZero();
}

@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveEveryConsecutiveLegFare() {
    Long invalidCount = jdbc.queryForObject(
        """
        WITH ordered_stops AS (
            SELECT
                ts.trip_id,
                ts.stop_id AS from_stop_id,
                lead(ts.stop_id) OVER (
                    PARTITION BY ts.trip_id
                    ORDER BY rs.stop_order
                ) AS to_stop_id
            FROM trip_stop ts
            JOIN route_stop rs
              ON rs.route_id = ts.route_id
             AND rs.stop_id = ts.stop_id
        )
        SELECT count(*)
        FROM ordered_stops os
        LEFT JOIN leg_fare lf
          ON lf.trip_id = os.trip_id
         AND lf.from_stop_id = os.from_stop_id
         AND lf.to_stop_id = os.to_stop_id
        WHERE os.to_stop_id IS NOT NULL
          AND lf.trip_id IS NULL
        """,
        Long.class
    );

    assertThat(invalidCount).isZero();
}

@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveChronologicalStopTimes() {
    Long invalidCount = jdbc.queryForObject(
        """
        WITH stop_times AS (
            SELECT
                ts.trip_id,
                rs.stop_order,
                ts.planned_arrival_time,
                ts.planned_departure_time,
                lag(
                    COALESCE(
                        ts.planned_departure_time,
                        ts.planned_arrival_time
                    )
                ) OVER (
                    PARTITION BY ts.trip_id
                    ORDER BY rs.stop_order
                ) AS previous_stop_time
            FROM trip_stop ts
            JOIN route_stop rs
              ON rs.route_id = ts.route_id
             AND rs.stop_id = ts.stop_id
        )
        SELECT count(*)
        FROM stop_times
        WHERE previous_stop_time IS NOT NULL
          AND COALESCE(
              planned_arrival_time,
              planned_departure_time
          ) <= previous_stop_time
        """,
        Long.class
    );

    assertThat(invalidCount).isZero();
}

@Test
@Sql("/fixtures/trip_demo.sql")
void demoTripsHaveOnlyForwardConsecutiveLegFares() {
    Long invalidCount = jdbc.queryForObject(
        """
        SELECT count(*)
        FROM leg_fare lf
        JOIN trip_stop from_ts
          ON from_ts.trip_id = lf.trip_id
         AND from_ts.stop_id = lf.from_stop_id
        JOIN route_stop from_rs
          ON from_rs.route_id = from_ts.route_id
         AND from_rs.stop_id = from_ts.stop_id
        JOIN trip_stop to_ts
          ON to_ts.trip_id = lf.trip_id
         AND to_ts.stop_id = lf.to_stop_id
        JOIN route_stop to_rs
          ON to_rs.route_id = to_ts.route_id
         AND to_rs.stop_id = to_ts.stop_id
        WHERE to_rs.stop_order <> from_rs.stop_order + 1
        """,
        Long.class
    );

    assertThat(invalidCount).isZero();
}

@Test
@Sql("/fixtures/trip_demo_runtime.sql")
void runtimeDemoTripsDepartTomorrowInVietnam() {
    java.time.LocalDate tomorrowInVietnam =
        java.time.LocalDate.now(
            java.time.ZoneId.of("Asia/Ho_Chi_Minh")
        ).plusDays(1);

    List<java.time.LocalDate> departureDates =
        jdbc.queryForList(
            """
            SELECT departure_date
            FROM trip
            ORDER BY trip_id
            """,
            java.time.LocalDate.class
        );

    assertThat(departureDates)
        .containsExactly(tomorrowInVietnam, tomorrowInVietnam);
}

}
