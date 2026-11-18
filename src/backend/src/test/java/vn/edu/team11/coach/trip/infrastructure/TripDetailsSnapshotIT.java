package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.team11.coach.trip.application.TripDetailsModels.Request;
import vn.edu.team11.coach.trip.application.TripDetailsService;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.Facts;
import vn.edu.team11.coach.trip.contract.TripStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@SpringBootTest
@Sql(scripts = "/fixtures/trip_demo.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "/fixtures/trip_snapshot_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class TripDetailsSnapshotIT {
    @Autowired private TripDetailsService service;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private JdbcTemplate jdbc;
    @MockitoBean(name = "tripDetailsClock") private Clock clock;
    @Test
    void writerCommitsBetweenHeadAndRemainingQueriesButReaderKeepsOneSnapshot() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        var headRead = new CountDownLatch(1);
        var firstRead = new AtomicBoolean(true);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var writer = executor.submit(() -> {
                assertThat(headRead.await(15, TimeUnit.SECONDS)).isTrue();
                var tx = new TransactionTemplate(transactions);
                tx.setTimeout(15);
                return tx.execute(status -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    int connection = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    jdbc.update("UPDATE trip SET status = 'Cancelled' WHERE trip_id = ?", DAY);
                    jdbc.update("UPDATE leg_fare SET fare_amount_vnd = 120000 WHERE trip_id = ? AND from_stop_id = ?", DAY, A);
                    jdbc.update("UPDATE trip_stop SET planned_departure_time = planned_departure_time + INTERVAL '10 minutes' WHERE trip_id = ? AND stop_id = ?", DAY, A);
                    jdbc.update("UPDATE stop SET stop_name = 'Updated A' WHERE stop_id = ?", A);
                    return connection;
                });
            });
            // Pause after the actual head SELECT, before Stop/Fare/layout SELECTs.
            // The service opens the reader transaction; no test-owned reader transaction.
            doAnswer(invocation -> {
                Object head = invocation.callRealMethod();
                if (firstRead.compareAndSet(true, false)) {
                    assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class)).isEqualTo("repeatable read");
                    assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("on");
                    int reader = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    headRead.countDown();
                    assertThat(writer.get(20, TimeUnit.SECONDS)).isNotEqualTo(reader);
                }
                return head;
            }).when(jdbc).query(contains("SELECT t.trip_id"), org.mockito.ArgumentMatchers.<RowMapper<Facts>>any(), eq(DAY));
            try {
                var old = service.details(new Request(DAY, A, C));
                assertThat(old.facts().status()).isEqualTo(TripStatus.Scheduled);
                assertThat(old.cutoff()).isEqualTo(CUTOFF);
                assertThat(old.segment().fareAmountVnd()).isEqualTo(250_000L);
                assertThat(old.stops().getFirst().name()).isEqualTo("Demo Stop A");
                assertThat(old.reasons()).isEmpty();
                var fresh = service.details(new Request(DAY, A, C));
                assertThat(fresh.facts().status()).isEqualTo(TripStatus.Cancelled);
                assertThat(fresh.cutoff()).isEqualTo(CUTOFF.plusSeconds(600));
                assertThat(fresh.segment().fareAmountVnd()).isEqualTo(270_000L);
                assertThat(fresh.stops().getFirst().name()).isEqualTo("Updated A");
            } finally {
                headRead.countDown();
            }
        }
    }
}
