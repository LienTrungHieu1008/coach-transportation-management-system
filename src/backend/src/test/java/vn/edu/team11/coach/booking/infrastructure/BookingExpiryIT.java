package vn.edu.team11.coach.booking.infrastructure;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.team11.coach.booking.application.BookingExpiryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.flyway.schemas=booking_expiry_task3",
        "spring.flyway.default-schema=booking_expiry_task3",
        "spring.datasource.hikari.schema=booking_expiry_task3"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingExpiryIT {
    private static final Instant AS_OF = Instant.parse("2026-11-19T00:10:00Z");
    private static final UUID LIVE = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd03");
    private static final UUID OVERDUE = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd04");

    @Autowired private BookingExpiryService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean(name = "bookingClock") private Clock clock;

    @BeforeEach
    void seedOwnedSchema() {
        new ResourceDatabasePopulator(new ClassPathResource("fixtures/identity_accounts.sql"),
                new ClassPathResource("fixtures/trip_demo.sql"),
                new ClassPathResource("fixtures/booking_availability.sql")).execute(dataSource);
        when(clock.instant()).thenReturn(AS_OF);
    }

    @AfterEach
    void clearOwnedSchema() {
        jdbc.execute("""
                truncate table booking_expiry_task3.seat_allocation, booking_expiry_task3.booking_seat,
                    booking_expiry_task3.booking, booking_expiry_task3.identity_account,
                    booking_expiry_task3.trip, booking_expiry_task3.trip_stop, booking_expiry_task3.leg_fare,
                    booking_expiry_task3.schedule, booking_expiry_task3.physical_seat, booking_expiry_task3.vehicle,
                    booking_expiry_task3.route_stop, booking_expiry_task3.stop, booking_expiry_task3.route cascade
                """);
    }

    @AfterAll
    void dropOwnedSchema() {
        jdbc.execute("drop schema booking_expiry_task3 cascade");
    }

    @Test
    void beforeBoundaryExpiresOnlyOverdueAndLeavesFutureHoldUntouched() {
        when(clock.instant()).thenReturn(AS_OF.minusNanos(1000));
        assertThat(service.expireDueBookings()).isEqualTo(1);
        assertThat(status(OVERDUE)).isEqualTo("Expired");
        assertThat(status(LIVE)).isEqualTo("Pending");
        assertThat(releases(LIVE)).containsExactly((Timestamp) null);
        assertThat(releases(OVERDUE)).containsExactly(Timestamp.from(AS_OF.minusNanos(1000)));
    }

    @Test
    void atBoundaryExpiresEverySeatOnceAndPreservesAllOtherFactsAndHistory() {
        addSecondSeatAndHistory();
        List<Map<String, Object>> before = snapshot("booking");
        List<Map<String, Object>> allocations = snapshot("seat_allocation");
        List<Map<String, Object>> seats = snapshot("booking_seat");
        List<Map<String, Object>> trips = snapshot("trip");
        List<Map<String, Object>> accounts = snapshot("identity_account");
        assertThat(service.expireDueBookings()).isEqualTo(2);
        assertThat(service.expireDueBookings()).isZero();
        assertThat(status(LIVE)).isEqualTo("Expired");
        assertThat(status(OVERDUE)).isEqualTo("Expired");
        assertThat(releases(LIVE)).containsExactly(Timestamp.from(AS_OF), Timestamp.from(AS_OF));
        assertThat(snapshot("booking_seat")).isEqualTo(seats);
        assertThat(snapshot("trip")).isEqualTo(trips);
        assertThat(snapshot("identity_account")).isEqualTo(accounts);
        for (Map<String, Object> row : before) {
            if (List.of(LIVE, OVERDUE).contains(row.get("booking_id"))) {
                row.put("status", "Expired");
            }
        }
        for (Map<String, Object> row : allocations) {
            if (List.of(LIVE, OVERDUE).contains(row.get("booking_id"))
                    && row.get("kind").equals("Hold") && row.get("released_at") == null) {
                row.put("released_at", Timestamp.from(AS_OF));
            }
        }
        assertThat(snapshot("booking")).isEqualTo(before);
        assertThat(snapshot("seat_allocation")).isEqualTo(allocations);
    }

    @Test
    void afterBoundaryUsesOneTruncatedReleaseInstantAndDoesNotExtendExpiry() {
        when(clock.instant()).thenReturn(AS_OF.plusNanos(1999));
        assertThat(service.expireDueBookings()).isEqualTo(2);
        assertThat(releases(LIVE)).containsExactly(Timestamp.from(AS_OF.plusNanos(1000)));
        assertThat(jdbc.queryForObject("select expires_at from booking where booking_id = ?", Timestamp.class, LIVE))
                .isEqualTo(Timestamp.from(AS_OF));
    }

    @Test
    void alreadyExpiredBookingIsUntouchedAndReleasedPendingHoldKeepsItsHistory() {
        jdbc.update("update booking set status = 'Expired' where booking_id = ?", OVERDUE);
        Timestamp oldRelease = Timestamp.from(AS_OF.minusSeconds(60));
        jdbc.update("update seat_allocation set released_at = ? where booking_id = ?", oldRelease, LIVE);
        List<Map<String, Object>> allocations = snapshot("seat_allocation");
        assertThat(service.expireDueBookings()).isEqualTo(1);
        assertThat(service.expireDueBookings()).isZero();
        assertThat(status(LIVE)).isEqualTo("Expired");
        assertThat(releases(LIVE)).containsExactly(oldRelease);
        assertThat(releases(OVERDUE)).containsExactly((Timestamp) null);
        assertThat(snapshot("seat_allocation")).isEqualTo(allocations);
    }

    @Test
    void concurrentServiceWorkersChangeEachBookingExactlyOnce() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runWorker(ready, start));
            var second = executor.submit(() -> runWorker(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(2);
        } finally {
            start.countDown();
        }
        assertThat(service.expireDueBookings()).isZero();
        assertThat(status(LIVE)).isEqualTo("Expired");
        assertThat(status(OVERDUE)).isEqualTo("Expired");
        assertThat(releases(LIVE)).containsExactly(Timestamp.from(AS_OF));
        assertThat(releases(OVERDUE)).containsExactly(Timestamp.from(AS_OF));
    }

    @Test
    void skipsBookingLockedByAnotherConnectionAndPicksItUpOnNextRun() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    connection.setAutoCommit(false);
                    try (var statement = connection.prepareStatement("select booking_id from booking where booking_id = ? for update")) {
                        statement.setObject(1, LIVE);
                        statement.executeQuery().close();
                        locked.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Lock release timed out");
                        }
                    } finally {
                        connection.rollback();
                    }
                }
                return null;
            });
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(service.expireDueBookings()).isEqualTo(1);
                assertThat(status(LIVE)).isEqualTo("Pending");
                assertThat(releases(LIVE)).containsExactly((Timestamp) null);
                assertThat(status(OVERDUE)).isEqualTo("Expired");
            } finally {
                release.countDown();
            }
            holder.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
        assertThat(service.expireDueBookings()).isEqualTo(1);
        assertThat(service.expireDueBookings()).isZero();
    }

    @Test
    void controlledAllocationFailureRollsBackWholeCallerAndExpiryTransaction() {
        assertThat(AopUtils.isAopProxy(service)).isTrue();
        List<Map<String, Object>> bookings = snapshot("booking");
        List<Map<String, Object>> allocations = snapshot("seat_allocation");
        jdbc.execute("""
                create function booking_expiry_task3.reject_release() returns trigger language plpgsql as $$
                begin
                    if (select count(*) from booking where status = 'Expired') <> 2
                        or (select count(*) from seat_allocation
                            where released_at = timestamptz '2026-11-19T00:10:00Z') <> 2 then
                        raise exception 'Expected expiry writes before failure';
                    end if;
                    raise exception 'Controlled release failure';
                end $$
                """);
        jdbc.execute("""
                create trigger reject_release after update on seat_allocation
                for each statement execute function booking_expiry_task3.reject_release()
                """);
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                jdbc.update("update booking set total_amount_vnd = total_amount_vnd + 1 where booking_id = ?", LIVE);
                service.expireDueBookings();
            })).isInstanceOf(DataAccessException.class).hasMessageContaining("Controlled release failure");
            assertThat(snapshot("booking")).isEqualTo(bookings);
            assertThat(snapshot("seat_allocation")).isEqualTo(allocations);
        } finally {
            jdbc.execute("drop trigger reject_release on seat_allocation");
            jdbc.execute("drop function booking_expiry_task3.reject_release()");
        }
        assertThat(service.expireDueBookings()).isEqualTo(2);
    }

    private int runWorker(CountDownLatch ready, CountDownLatch start) throws Exception {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            jdbc.queryForObject("select pg_backend_pid()", Integer.class);
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Worker start timed out");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
            return service.expireDueBookings();
        });
    }

    private void addSecondSeatAndHistory() {
        jdbc.update("""
                insert into booking_seat values ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee06', ?,
                    '77777777-7777-4777-8777-777777777777', 'Synthetic Traveller One', 150000)
                """, LIVE);
        jdbc.update("update booking set total_amount_vnd = 300000 where booking_id = ?", LIVE);
        jdbc.execute("""
                insert into seat_allocation
                select s.booking_seat_id, s.booking_seat_id, b.booking_id, b.trip_id, s.physical_seat_id,
                    b.from_stop_index, b.to_stop_index, 'Hold', b.expires_at, null
                from booking_seat s join booking b using (booking_id)
                where s.booking_seat_id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee06'
                """);
        jdbc.execute("""
                insert into seat_allocation
                select 'ffffffff-ffff-4fff-8fff-fffffffffff1', booking_seat_id, booking_id, trip_id,
                    physical_seat_id, from_stop_index, to_stop_index, kind, expires_at,
                    timestamptz '2026-11-19T00:01:00Z'
                from seat_allocation where booking_id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd04'
                """);
        jdbc.execute("""
                insert into seat_allocation
                select 'ffffffff-ffff-4fff-8fff-fffffffffff2', booking_seat_id, booking_id, trip_id,
                    physical_seat_id, from_stop_index, to_stop_index, 'Confirmed', expires_at, null
                from seat_allocation where seat_allocation_id = 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeee04'
                """);
        jdbc.execute("update seat_allocation set released_at = null where booking_id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd05'");
    }

    private String status(UUID booking) {
        return jdbc.queryForObject("select status from booking where booking_id = ?", String.class, booking);
    }

    private List<Timestamp> releases(UUID booking) {
        return jdbc.queryForList("""
                select released_at from seat_allocation where booking_id = ? and kind = 'Hold'
                order by seat_allocation_id
                """, Timestamp.class, booking);
    }

    private List<Map<String, Object>> snapshot(String table) {
        return jdbc.queryForList("select * from " + table + " order by 1");
    }
}
