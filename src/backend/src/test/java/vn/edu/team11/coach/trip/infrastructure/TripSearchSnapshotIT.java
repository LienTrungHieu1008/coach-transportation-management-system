package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.team11.coach.trip.application.TripSearchModels.Request;
import vn.edu.team11.coach.trip.application.TripSearchService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@SpringBootTest
@Sql(scripts = "/fixtures/trip_demo.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "/fixtures/trip_snapshot_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class TripSearchSnapshotIT {
    @Autowired private TripSearchService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean(name = "tripSearchClock") private Clock clock;
    @MockitoSpyBean private PostgresTripSearchRepository repository;

    @Test
    void applicationTransactionKeepsCountPageAndFactsConsistentAcrossWriterCommit() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        CountDownLatch snapshotReady = new CountDownLatch(1);
        AtomicBoolean firstRead = new AtomicBoolean(true);
        AtomicInteger readerConnection = new AtomicInteger();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var writer = executor.submit(() -> {
                assertThat(snapshotReady.await(15, TimeUnit.SECONDS)).isTrue();
                var transaction = new TransactionTemplate(transactionManager);
                transaction.setTimeout(15);
                return transaction.execute(status -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    int connection = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    jdbc.update("UPDATE leg_fare SET fare_amount_vnd = 120000 WHERE trip_id = ? AND from_stop_id = ?", DAY, A);
                    jdbc.update("UPDATE trip SET status = 'Cancelled' WHERE trip_id = ?", NIGHT);
                    jdbc.update("UPDATE stop SET stop_name = 'Updated Stop A' WHERE stop_id = ?", A);
                    return connection;
                });
            });
            // The spy only establishes a deterministic interleaving; all production SQL
            // still executes against PostgreSQL. No test-owned reader transaction masks
            // the service's isolation/readOnly annotations.
            PostgresTripSearchRepository spyTarget = AopTestUtils.getUltimateTargetObject(repository);
            doAnswer(invocation -> {
                if (firstRead.compareAndSet(true, false)) {
                    assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class)).isEqualTo("repeatable read");
                    assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("on");
                    readerConnection.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip", Long.class)).isEqualTo(2);
                    snapshotReady.countDown();
                    assertThat(writer.get(20, TimeUnit.SECONDS)).isNotEqualTo(readerConnection.get());
                }
                return invocation.callRealMethod();
            }).when(spyTarget).load(any(), any());
            try {
                var request = new Request(A, B, DATE, 0, 1);
                var old = service.search(request);
                assertThat(old.totalElements()).isEqualTo(2);
                assertThat(old.totalPages()).isEqualTo(2);
                assertThat(old.items().getFirst().fareAmountVnd()).isEqualTo(100_000);
                assertThat(old.items().getFirst().fromStop().name()).isEqualTo("Demo Stop A");
                var fresh = service.search(request);
                assertThat(fresh.totalElements()).isEqualTo(1);
                assertThat(fresh.totalPages()).isEqualTo(1);
                assertThat(fresh.items().getFirst().fareAmountVnd()).isEqualTo(120_000);
                assertThat(fresh.items().getFirst().fromStop().name()).isEqualTo("Updated Stop A");
            } finally {
                snapshotReady.countDown();
            }
        }
    }
}
