package vn.edu.team11.coach.identity.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AccountSchemaIT {
    private static final String INSERT = """
            insert into identity_account
                (account_id, email, password_hash, full_name, phone, role, status)
            values (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final List<String> COLUMNS = List.of(
            "account_id", "email", "password_hash", "full_name", "phone", "role", "status");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void freshDatabaseHasBothMigrationsUuidPrimaryKeyAndNoSeededAccounts() {
        assertThat(jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null",
                String.class)).contains("1", "2");
        assertThat(jdbc.queryForObject("select count(*) from identity_account", Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'identity_account'
                and column_name = 'account_id'
                """, String.class)).isEqualTo("uuid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSENGER", "STAFF", "ADMINISTRATOR"})
    void acceptsOnlyThePublishedRoles(String role) {
        Object[] account = accountValues();
        account[5] = role;
        jdbc.update(INSERT, account);
        assertThat(jdbc.queryForObject(
                "select role from identity_account where account_id = ?", String.class, account[0]))
                .isEqualTo(role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "DISABLED"})
    void acceptsThePublishedAccountStatuses(String status) {
        Object[] account = accountValues();
        account[6] = status;
        jdbc.update(INSERT, account);
        assertThat(jdbc.queryForObject(
                "select status from identity_account where account_id = ?", String.class, account[0]))
                .isEqualTo(status);
    }

    @Test
    void rejectsDuplicateAccountId() {
        Object[] first = accountValues();
        jdbc.update(INSERT, first);
        Object[] duplicate = accountValues();
        duplicate[0] = first[0];
        assertSqlState(() -> jdbc.update(INSERT, duplicate), "23505");
    }

    @ParameterizedTest
    @ValueSource(strings = {"existing@example.invalid", "EXISTING@example.invalid"})
    void rejectsDuplicateEmailIncludingDifferentCase(String duplicateEmail) {
        Object[] first = accountValues();
        first[1] = "existing@example.invalid";
        jdbc.update(INSERT, first);
        Object[] duplicate = accountValues();
        duplicate[1] = duplicateEmail;
        assertSqlState(() -> jdbc.update(INSERT, duplicate), "23505");
    }

    @ParameterizedTest
    @ValueSource(strings = {"account_id", "email", "password_hash", "full_name", "phone", "role", "status"})
    void rejectsNullRequiredFields(String column) {
        Object[] account = accountValues();
        account[COLUMNS.indexOf(column)] = null;
        assertSqlState(() -> jdbc.update(INSERT, account), "23502");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "email|", "email|   ",
            "password_hash|", "password_hash|   ",
            "full_name|", "full_name|   ",
            "phone|", "phone|   "
    }, delimiter = '|', emptyValue = "", nullValues = "<null>", ignoreLeadingAndTrailingWhitespace = false)
    void rejectsEmptyOrBlankRequiredText(String column, String value) {
        Object[] account = accountValues();
        // CsvSource represents an unquoted empty trailing cell as null.
        account[COLUMNS.indexOf(column)] = value == null ? "" : value;
        assertSqlState(() -> jdbc.update(INSERT, account), "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"passenger", "ADMIN", "UNKNOWN"})
    void rejectsUnknownRoles(String role) {
        Object[] account = accountValues();
        account[5] = role;
        assertSqlState(() -> jdbc.update(INSERT, account), "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "LOCKED", "UNKNOWN"})
    void rejectsUnknownStatuses(String status) {
        Object[] account = accountValues();
        account[6] = status;
        assertSqlState(() -> jdbc.update(INSERT, account), "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {" existing@example.invalid", "existing@example.invalid "})
    void rejectsSurroundingEmailWhitespace(String email) {
        Object[] account = accountValues();
        account[1] = email;
        assertSqlState(() -> jdbc.update(INSERT, account), "23514");
    }

    @Test
    void bookingCanReferenceTheAccountKeyAndCannotReferenceAnUnknownAccount() {
        Object[] account = accountValues();
        jdbc.update(INSERT, account);
        jdbc.execute("""
                create table i1_booking_fk_probe (
                    passenger_account_id uuid not null references identity_account(account_id)
                )
                """);
        jdbc.update("insert into i1_booking_fk_probe values (?)", account[0]);
        assertThat(jdbc.queryForObject("select count(*) from i1_booking_fk_probe", Long.class)).isEqualTo(1L);
        assertSqlState(() -> jdbc.update("insert into i1_booking_fk_probe values (?)", UUID.randomUUID()), "23503");
    }

    @Test
    @Sql("/fixtures/identity_accounts.sql")
    void separateFixtureProvidesDisabledPassengersWithoutACredential() {
        assertThat(jdbc.queryForList("select account_id from identity_account order by account_id", UUID.class))
                .containsExactly(
                        UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1"),
                        UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2"));
        assertThat(jdbc.queryForList("select status from identity_account", String.class))
                .containsOnly("DISABLED");
        assertThat(jdbc.queryForList("select role from identity_account", String.class))
                .containsOnly("PASSENGER");
        assertThat(jdbc.queryForList("select password_hash from identity_account", String.class))
                .containsOnly("!DISABLED_DEMO_NO_CREDENTIAL");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void upgradeFromV1PreservesSessionDataAndReapplyingMigrationsDoesNothing(@TempDir Path futureMigrations)
            throws IOException {
        // A newer test-only version exercises this historical upgrade boundary outside the application classpath.
        Files.writeString(futureMigrations.resolve("V999__test_future_migration.sql"),
                "create table i1_future_migration_probe (marker integer);");
        // Isolated schema on the separate PostgreSQL test service, not the application schema.
        String schema = "i1_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        assertThat(schema).matches("i1_upgrade_[0-9a-f]{32}");
        Flyway initial = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration", "filesystem:" + futureMigrations)
                .schemas(schema).defaultSchema(schema).target("1").load();
        try {
            initial.migrate();
            String sessionId = UUID.randomUUID().toString();
            jdbc.update("insert into " + schema + ".spring_session "
                            + "(primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time) "
                            + "values (?, ?, 1, 1, 1800, 1800001)", sessionId, sessionId);
            byte[] attribute = new byte[]{1, 2, 3};
            jdbc.update("insert into " + schema + ".spring_session_attributes values (?, ?, ?)",
                    sessionId, "preserved", attribute);

            Flyway upgrade = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration", "filesystem:" + futureMigrations)
                    .schemas(schema).defaultSchema(schema).target("2").load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForList("select version from " + schema
                    + ".flyway_schema_history where success and version is not null", String.class))
                    .containsExactly("1", "2");
            assertThat(jdbc.queryForObject("select session_id from " + schema + ".spring_session", String.class))
                    .isEqualTo(sessionId);
            assertThat(jdbc.queryForObject("select attribute_bytes from " + schema
                    + ".spring_session_attributes where attribute_name = 'preserved'", byte[].class))
                    .containsExactly(attribute);
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".identity_account", Long.class))
                    .isZero();
            assertThat(jdbc.queryForObject("select to_regclass(?)::text", String.class,
                    schema + ".i1_future_migration_probe")).isNull();
            assertThat(upgrade.migrate().migrationsExecuted).isZero();
            upgrade.validate();
        } finally {
            // Generated, validated identifier owned only by this test; never drops public or a supplied schema.
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }

    private Object[] accountValues() {
        return new Object[]{
                UUID.randomUUID(), "test-" + UUID.randomUUID() + "@example.invalid",
                "!DISABLED_TEST_NO_CREDENTIAL", "Synthetic Account", "0000000000", "PASSENGER", "DISABLED"
        };
    }

    private void assertSqlState(Runnable write, String expectedState) {
        DataIntegrityViolationException error = catchThrowableOfType(write::run, DataIntegrityViolationException.class);
        assertThat(error).isNotNull();
        assertThat(error.getMostSpecificCause()).isInstanceOf(SQLException.class);
        assertThat(((SQLException) error.getMostSpecificCause()).getSQLState()).isEqualTo(expectedState);
    }
}
