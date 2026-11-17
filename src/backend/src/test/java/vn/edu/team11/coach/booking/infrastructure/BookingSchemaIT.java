package vn.edu.team11.coach.booking.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Sql({"/fixtures/identity_accounts.sql", "/fixtures/trip_demo.sql"})
class BookingSchemaIT {
    private static final UUID ACCOUNT = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_TRIP = UUID.fromString("11111111-1111-4111-8111-111111111112");
    private static final UUID STOP_A = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID STOP_B = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID STOP_C = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final UUID SEAT_A1 = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final UUID SEAT_A2 = UUID.fromString("88888888-8888-4888-8888-888888888888");
    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-11-19T00:00:00Z");
    private static final OffsetDateTime EXPIRES = OffsetDateTime.parse("2026-11-19T00:10:00Z");
    private static final String INSERT_BOOKING = """
            insert into booking (booking_id, passenger_account_id, trip_id, from_stop_id, to_stop_id,
                from_stop_index, to_stop_index, status, total_amount_vnd, hold_created_at, expires_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_SEAT = """
            insert into booking_seat (booking_seat_id, booking_id, physical_seat_id, traveller_name, fare_amount_vnd)
            values (?, ?, ?, ?, ?)
            """;
    private static final String INSERT_ALLOCATION = """
            insert into seat_allocation (seat_allocation_id, booking_seat_id, booking_id, trip_id,
                physical_seat_id, from_stop_index, to_stop_index, kind, expires_at, released_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void freshV1ThroughV4HasNoBookingSeedOrGeneratedIds() throws SQLException {
        String schema = isolatedSchema();
        try {
            assertThat(migration(schema, "4").migrate().migrationsExecuted).isEqualTo(4);
            try (Connection connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                JdbcTemplate isolated = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                assertThat(isolated.queryForList("select version from flyway_schema_history where version is not null order by installed_rank",
                        String.class)).containsExactly("1", "2", "3", "4");
                for (String table : List.of("booking", "booking_seat", "seat_allocation")) {
                    assertThat(isolated.queryForObject("select count(*) from " + table, Long.class)).isZero();
                }
                assertThat(isolated.queryForList("""
                        select column_default from information_schema.columns
                        where table_schema = ? and table_name in ('booking', 'booking_seat', 'seat_allocation')
                            and data_type = 'uuid'
                        """, String.class, schema)).isNotEmpty().containsOnlyNulls();
            }
            assertThat(migration(schema, "4").migrate().migrationsExecuted).isZero();
            migration(schema, "4").validate();
        } finally {
            dropIsolatedSchema(schema);
        }
    }

    @Test
    void upgradeFromV3PreservesAccountsSessionsAndAllTripRows() throws SQLException {
        String schema = isolatedSchema();
        try {
            assertThat(migration(schema, "3").migrate().migrationsExecuted).isEqualTo(3);
            try (Connection connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                new ResourceDatabasePopulator(new ClassPathResource("fixtures/identity_accounts.sql"),
                        new ClassPathResource("fixtures/trip_demo.sql")).populate(connection);
                JdbcTemplate isolated = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                String session = UUID.randomUUID().toString();
                isolated.update("""
                        insert into spring_session
                            (primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time)
                        values (?, ?, 1, 1, 1800, 1800001)
                        """, session, session);
                isolated.update("insert into spring_session_attributes values (?, ?, ?)",
                        session, "preserved", new byte[]{1, 2, 3});
                Map<String, List<String>> before = legacyRows(isolated);
                assertThat(migration(schema, "4").migrate().migrationsExecuted).isEqualTo(1);
                assertThat(legacyRows(isolated)).isEqualTo(before);
                assertThat(isolated.queryForObject("select count(*) from booking", Long.class)).isZero();
                assertThat(migration(schema, "4").migrate().migrationsExecuted).isZero();
            }
        } finally {
            dropIsolatedSchema(schema);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pending", "Confirmed", "Expired", "Cancelled"})
    void acceptsPublishedBookingStatusesAndBigintAmounts(String status) {
        Object[] booking = bookingValues();
        booking[7] = status;
        booking[8] = 3_000_000_000L;
        jdbc.update(INSERT_BOOKING, booking);
        assertThat(jdbc.queryForObject("select total_amount_vnd from booking where booking_id = ?",
                Long.class, booking[0])).isEqualTo(3_000_000_000L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void rejectsUnknownBookingForeignKeys(int column) {
        Object[] booking = bookingValues();
        booking[column] = UUID.randomUUID();
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23503");
    }

    @Test
    void rejectsKnownStopThatIsNotServedByTheBookingTrip() {
        UUID stop = UUID.randomUUID();
        jdbc.update("insert into stop (stop_id, stop_name, address) values (?, 'Other', 'Other')", stop);
        Object[] booking = bookingValues();
        booking[3] = stop;
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23503");
    }

    @ParameterizedTest
    @CsvSource({"-1,1", "1,1", "2,1"})
    void rejectsInvalidBookingSegments(int from, int to) {
        Object[] booking = bookingValues();
        booking[5] = from;
        booking[6] = to;
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23514");
    }

    @ParameterizedTest
    @CsvSource({"7,Unknown", "8,-1"})
    void rejectsInvalidBookingStatusOrNegativeTotal(int column, String value) {
        Object[] booking = bookingValues();
        booking[column] = column == 8 ? Long.valueOf(value) : value;
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23514");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 599, 601})
    void rejectsHoldLifetimeOtherThanExactlyTenMinutes(long seconds) {
        Object[] booking = bookingValues();
        booking[10] = CREATED.plusSeconds(seconds);
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"infinity", "-infinity"})
    void rejectsNonFiniteHoldTimestamps(String instant) {
        UUID booking = insertBooking();
        assertSqlState(() -> jdbc.update("""
                update booking set hold_created_at = cast(? as timestamptz), expires_at = cast(? as timestamptz)
                where booking_id = ?
                """, instant, instant, booking), "23514");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void rejectsNullBookingFields(int column) {
        Object[] booking = bookingValues();
        booking[column] = null;
        assertSqlState(() -> jdbc.update(INSERT_BOOKING, booking), "23502");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void rejectsUnknownBookingSeatParentOrPhysicalSeat(int column) {
        Object[] seat = seatValues(insertBooking(), SEAT_A1);
        seat[column] = UUID.randomUUID();
        assertSqlState(() -> jdbc.update(INSERT_SEAT, seat), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n", " \t\n "})
    void rejectsBlankTravellerName(String traveller) {
        Object[] seat = seatValues(insertBooking(), SEAT_A1);
        seat[3] = traveller;
        assertSqlState(() -> jdbc.update(INSERT_SEAT, seat), "23514");
    }

    @Test
    void rejectsNegativeSeatFare() {
        Object[] seat = seatValues(insertBooking(), SEAT_A1);
        seat[4] = -1L;
        assertSqlState(() -> jdbc.update(INSERT_SEAT, seat), "23514");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void rejectsNullBookingSeatFields(int column) {
        Object[] seat = seatValues(insertBooking(), SEAT_A1);
        seat[column] = null;
        assertSqlState(() -> jdbc.update(INSERT_SEAT, seat), "23502");
    }

    @Test
    void rejectsDuplicateSeatWithinBooking() {
        UUID booking = insertBooking();
        jdbc.update(INSERT_SEAT, seatValues(booking, SEAT_A1));
        assertSqlState(() -> jdbc.update(INSERT_SEAT, seatValues(booking, SEAT_A1)), "23505");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hold", "Confirmed"})
    void acceptsAllocationWithMatchingParentSeatTripSegmentAndExpiry(String kind) {
        Object[] allocation = allocationValues();
        allocation[7] = kind;
        jdbc.update(INSERT_ALLOCATION, allocation);
        assertThat(jdbc.queryForObject("select kind from seat_allocation where seat_allocation_id = ?",
                String.class, allocation[0])).isEqualTo(kind);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void rejectsUnknownAllocationForeignKeys(int column) {
        Object[] allocation = allocationValues();
        allocation[column] = UUID.randomUUID();
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hold", "Confirmed"})
    void rejectsAllocationBelongingToAnotherExistingBooking(String kind) {
        Object[] allocation = allocationValues();
        allocation[2] = insertBooking();
        allocation[7] = kind;
        allocation[8] = kind.equals("Confirmed") ? null : EXPIRES;
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23503");
    }

    @ParameterizedTest
    @CsvSource({"Hold,3", "Hold,4", "Hold,6", "Confirmed,3", "Confirmed,4", "Confirmed,6"})
    void rejectsAllocationTripSeatOrSegmentMismatchEvenWithNullConfirmedExpiry(String kind, int column) {
        Object[] allocation = allocationValues();
        allocation[column] = switch (column) {
            case 3 -> OTHER_TRIP;
            case 4 -> SEAT_A2;
            default -> 2;
        };
        allocation[7] = kind;
        allocation[8] = kind.equals("Confirmed") ? null : EXPIRES;
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hold", "Confirmed"})
    void rejectsNonNullAllocationExpiryThatDiffersFromBooking(String kind) {
        Object[] allocation = allocationValues();
        allocation[7] = kind;
        allocation[8] = EXPIRES.plusSeconds(1);
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23503");
    }

    @Test
    void rejectsHoldWithoutExpiry() {
        Object[] allocation = allocationValues();
        allocation[8] = null;
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23514");
    }

    @Test
    void acceptsConfirmedWithoutExpiryAndReleasedAllocation() {
        Object[] allocation = allocationValues();
        allocation[7] = "Confirmed";
        allocation[8] = null;
        allocation[9] = EXPIRES.plusSeconds(1);
        jdbc.update(INSERT_ALLOCATION, allocation);
        assertThat(jdbc.queryForObject("select expires_at is null and released_at is not null "
                + "from seat_allocation where seat_allocation_id = ?", Boolean.class, allocation[0])).isTrue();
    }

    @Test
    void rejectsUnknownAllocationKind() {
        Object[] allocation = allocationValues();
        allocation[7] = "Unknown";
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23514");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void rejectsNullAllocationRequiredFields(int column) {
        Object[] allocation = allocationValues();
        allocation[column] = null;
        assertSqlState(() -> jdbc.update(INSERT_ALLOCATION, allocation), "23502");
    }

    @Test
    void allowsSamePhysicalSeatOnAdjacentSegmentsForDifferentBookings() {
        Object[] first = allocationValues();
        jdbc.update(INSERT_ALLOCATION, first);
        Object[] secondBooking = bookingValues();
        secondBooking[3] = STOP_B;
        secondBooking[4] = STOP_C;
        secondBooking[5] = 1;
        secondBooking[6] = 2;
        jdbc.update(INSERT_BOOKING, secondBooking);
        Object[] secondSeat = seatValues((UUID) secondBooking[0], SEAT_A1);
        jdbc.update(INSERT_SEAT, secondSeat);
        Object[] second = {UUID.randomUUID(), secondSeat[0], secondBooking[0], TRIP, SEAT_A1,
                1, 2, "Hold", EXPIRES, null};
        jdbc.update(INSERT_ALLOCATION, second);
        assertThat(jdbc.queryForObject("select count(*) from seat_allocation where physical_seat_id = ?",
                Long.class, SEAT_A1)).isEqualTo(2L);
    }

    @Test
    void hasCandidateReadAndPendingExpiryIndexes() {
        List<String> indexes = jdbc.queryForList("""
                select indexdef from pg_indexes where schemaname = current_schema()
                    and tablename in ('booking', 'seat_allocation')
                """, String.class);
        assertThat(indexes).anySatisfy(index -> assertThat(index)
                .contains("(trip_id, physical_seat_id, from_stop_index, to_stop_index)")
                .contains("released_at IS NULL"));
        assertThat(indexes).anySatisfy(index -> assertThat(index)
                .contains("(expires_at)").contains("status = 'Pending'"));
    }

    private Object[] bookingValues() {
        return new Object[]{UUID.randomUUID(), ACCOUNT, TRIP, STOP_A, STOP_B,
                0, 1, "Pending", 100000L, CREATED, EXPIRES};
    }

    private UUID insertBooking() {
        Object[] booking = bookingValues();
        jdbc.update(INSERT_BOOKING, booking);
        return (UUID) booking[0];
    }

    private Object[] seatValues(UUID booking, UUID seat) {
        return new Object[]{UUID.randomUUID(), booking, seat, "Synthetic Traveller", 100000L};
    }

    private Object[] allocationValues() {
        UUID booking = insertBooking();
        Object[] seat = seatValues(booking, SEAT_A1);
        jdbc.update(INSERT_SEAT, seat);
        return new Object[]{UUID.randomUUID(), seat[0], booking, TRIP, SEAT_A1, 0, 1, "Hold", EXPIRES, null};
    }

    private void assertSqlState(Runnable write, String state) {
        DataIntegrityViolationException error = catchThrowableOfType(write::run, DataIntegrityViolationException.class);
        assertThat(error).isNotNull();
        assertThat(error.getMostSpecificCause()).isInstanceOf(SQLException.class);
        assertThat(((SQLException) error.getMostSpecificCause()).getSQLState()).isEqualTo(state);
    }

    private String isolatedSchema() {
        String schema = "booking_migration_" + UUID.randomUUID().toString().replace("-", "");
        assertThat(schema).matches("booking_migration_[0-9a-f]{32}");
        return schema;
    }

    private Flyway migration(String schema, String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(target).load();
    }

    private Map<String, List<String>> legacyRows(JdbcTemplate isolated) {
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (String table : List.of("identity_account", "spring_session", "spring_session_attributes", "route",
                "stop", "route_stop", "vehicle", "physical_seat", "schedule", "trip", "trip_stop", "leg_fare")) {
            rows.put(table, isolated.queryForList("select to_jsonb(t)::text from " + table
                    + " t order by to_jsonb(t)::text", String.class));
        }
        return rows;
    }

    private void dropIsolatedSchema(String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
