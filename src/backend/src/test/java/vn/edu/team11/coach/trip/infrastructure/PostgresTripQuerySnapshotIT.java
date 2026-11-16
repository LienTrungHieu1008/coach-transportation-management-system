package vn.edu.team11.coach.trip.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.team11.coach.trip.contract.TripQueryPort;

/** Tests the Trip adapter's participation in a caller snapshot, not Booking integration. */
@SpringBootTest
@Sql(scripts = "/fixtures/trip_demo.sql",
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "/fixtures/trip_snapshot_cleanup.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class PostgresTripQuerySnapshotIT {

    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STOP_A = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID STOP_B = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID SEAT_A1 = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final Instant AS_OF = Instant.parse("2026-11-20T00:00:00Z");

    @Autowired
    private TripQueryPort port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void callerSnapshotSurvivesConcurrentCommitAndNewTransactionSeesUpdate() {
        // No test-managed outer transaction: the fixture must be committed for both connections.
        TransactionTemplate reader = new TransactionTemplate(transactionManager);
        reader.setReadOnly(true);
        reader.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        reader.setTimeout(30);
        CountDownLatch snapshotRead = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<Integer> writer = executor.submit(() -> {
                assertThat(snapshotRead.await(15, TimeUnit.SECONDS))
                        .as("reader must establish the snapshot before writer updates").isTrue();
                TransactionTemplate write = new TransactionTemplate(transactionManager);
                write.setTimeout(15);
                // Completion of execute means the write has COMMITTED, not just issued UPDATE.
                return write.execute(status -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    Integer connectionId = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    assertThat(jdbc.update("""
                            UPDATE physical_seat SET seat_code = 'A1-updated'
                            WHERE physical_seat_id = ?
                            """, SEAT_A1)).isEqualTo(1);
                    return connectionId;
                });
            });

            try {
                reader.executeWithoutResult(status -> {
                    Integer readerConnection = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    assertThat(readSeatLabel()).isEqualTo("A1");
                    snapshotRead.countDown();
                    assertThat(awaitCommit(writer)).as("writer uses a different PostgreSQL connection")
                            .isNotNull().isNotEqualTo(readerConnection);
                    assertThat(readSeatLabel()).as("same caller snapshot after writer commit")
                            .isEqualTo("A1");
                });

                String newLabel = reader.execute(status -> readSeatLabel());
                assertThat(newLabel)
                        .as("new caller transaction sees committed layout data")
                        .isEqualTo("A1-updated");
            } finally {
                // Unblock the worker on an early assertion failure before executor.close joins it.
                snapshotRead.countDown();
            }
        }
    }

    private String readSeatLabel() {
        return port.getSeatAvailabilityFacts(TRIP, STOP_A, STOP_B, AS_OF)
                .seatLayout().seats().stream()
                .filter(seat -> seat.physicalSeatId().equals(SEAT_A1))
                .findFirst().orElseThrow().label();
    }

    private static Integer awaitCommit(Future<Integer> writer) {
        try {
            return writer.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while awaiting writer commit", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new AssertionError("Writer did not commit successfully", exception);
        }
    }
}
