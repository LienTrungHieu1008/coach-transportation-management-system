package vn.edu.team11.coach.booking.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.team11.coach.booking.api.SeatAvailabilityController;
import vn.edu.team11.coach.booking.api.SeatAvailabilityExceptionHandler;
import vn.edu.team11.coach.booking.api.SeatAvailabilityResponse;
import vn.edu.team11.coach.booking.application.SeatAvailabilityApplicationService;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripSeatFacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.schemas=booking_availability_task5",
        "spring.flyway.default-schema=booking_availability_task5",
        "spring.datasource.hikari.schema=booking_availability_task5"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeatAvailabilityIT {
    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID A = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID B = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID C = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final UUID UNKNOWN = UUID.fromString("00000000-0000-4000-8000-000000000000");
    private static final UUID LIVE = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddd03");
    private static final Instant AS_OF = Instant.parse("2026-11-19T00:08:00Z");
    private static final Instant EXPIRY = Instant.parse("2026-11-19T00:10:00Z");
    private static final Instant CUTOFF = Instant.parse("2026-11-20T01:30:00Z");
    private static final List<UUID> SEATS = List.of(
            UUID.fromString("77777777-7777-4777-8777-777777777777"),
            UUID.fromString("88888888-8888-4888-8888-888888888888"),
            UUID.fromString("77777777-7777-4777-8777-777777777778"),
            UUID.fromString("77777777-7777-4777-8777-777777777779"),
            UUID.fromString("77777777-7777-4777-8777-777777777780"),
            UUID.fromString("77777777-7777-4777-8777-777777777781"));

    @Autowired private SeatAvailabilityApplicationService service;
    @Autowired private TripQueryPort port;
    @Autowired private BookingRepository repository;
    @Autowired private SeatAvailabilityController controller;
    @Autowired private SeatAvailabilityExceptionHandler errors;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean(name = "bookingClock") private Clock clock;

    private volatile Map<String, Object> tripConnection;
    private volatile Map<String, Object> bookingConnection;
    private volatile Instant observedAsOf;
    private volatile TripSeatFacts observedFacts;
    private volatile Runnable afterFacts;
    private MethodInterceptor tripObserver;
    private MethodInterceptor bookingObserver;
    private MockMvc mvc;

    @BeforeEach
    void seedOwnedSchemaAndObserveRealProxies() {
        new ResourceDatabasePopulator(new ClassPathResource("fixtures/identity_accounts.sql"),
                new ClassPathResource("fixtures/trip_demo.sql"),
                new ClassPathResource("fixtures/booking_availability.sql")).execute(dataSource);
        when(clock.instant()).thenReturn(AS_OF);
        assertThat(AopUtils.isAopProxy(service)).isTrue();
        assertThat(AopUtils.getTargetClass(port).getName())
                .isEqualTo("vn.edu.team11.coach.trip.infrastructure.PostgresTripQueryAdapter");
        assertThat(AopUtils.getTargetClass(repository)).isEqualTo(BookingRepository.class);
        tripConnection = null;
        bookingConnection = null;
        observedAsOf = null;
        observedFacts = null;
        afterFacts = null;
        tripObserver = invocation -> {
            if (!invocation.getMethod().getName().equals("getSeatAvailabilityFacts")) {
                return invocation.proceed();
            }
            tripConnection = connectionState();
            observedAsOf = (Instant) invocation.getArguments()[3];
            Object result = invocation.proceed();
            observedFacts = (TripSeatFacts) result;
            if (afterFacts != null) {
                afterFacts.run();
            }
            return result;
        };
        bookingObserver = invocation -> {
            if (invocation.getMethod().getName().equals("findCandidates")) {
                bookingConnection = connectionState();
            }
            return invocation.proceed();
        };
        ((Advised) port).addAdvice(tripObserver);
        ((Advised) repository).addAdvice(bookingObserver);
        // Real controller/service/ports, with no security filters: this is not public-route acceptance.
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(errors).build();
    }

    @AfterEach
    void removeObserversAndClearOwnedSchema() {
        ((Advised) port).removeAdvice(tripObserver);
        ((Advised) repository).removeAdvice(bookingObserver);
        jdbc.execute("""
                truncate table booking_availability_task5.seat_allocation, booking_availability_task5.booking_seat,
                    booking_availability_task5.booking, booking_availability_task5.identity_account,
                    booking_availability_task5.trip, booking_availability_task5.trip_stop,
                    booking_availability_task5.leg_fare, booking_availability_task5.schedule,
                    booking_availability_task5.physical_seat, booking_availability_task5.vehicle,
                    booking_availability_task5.route_stop, booking_availability_task5.stop,
                    booking_availability_task5.route cascade
                """);
    }

    @AfterAll
    void dropOwnedSchema() {
        jdbc.execute("drop schema booking_availability_task5 cascade");
    }

    @Test
    void realFactsAndAllocationsReturnCompleteOrderedAcAndBcMaps() {
        var ac = service.getSeatAvailability(TRIP, A, C);
        assertMap(ac, "FREE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "FREE", "FREE");
        assertThat(ac.layout().id()).isEqualTo(UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"));
        assertThat(ac.layout().seats()).containsExactly(
                new LayoutSeat(SEATS.get(0), "A1", 0, 0, 0),
                new LayoutSeat(SEATS.get(1), "A2", 0, 0, 2),
                new LayoutSeat(SEATS.get(2), "B1", 0, 1, 0),
                new LayoutSeat(SEATS.get(3), "B2", 0, 1, 2),
                new LayoutSeat(SEATS.get(4), "C1", 0, 2, 0),
                new LayoutSeat(SEATS.get(5), "C2", 0, 2, 2));
        assertThat(ac.seats()).extracting(SeatAvailabilityResponse.SeatState::expiresAt)
                .containsExactly(null, null, null, EXPIRY, null, null);
        assertThat(ac.tripId()).isEqualTo(TRIP);
        assertThat(ac.fromStopId()).isEqualTo(A);
        assertThat(ac.toStopId()).isEqualTo(C);
        assertThat(ac.bookingClosesAt()).isEqualTo(CUTOFF);
        assertReadOnlySnapshot();

        var bc = service.getSeatAvailability(TRIP, B, C);
        assertMap(bc, "FREE", "FREE", "UNAVAILABLE", "UNAVAILABLE", "FREE", "FREE");
        assertThat(bc.fromStopId()).isEqualTo(B);
        assertThat(bc.toStopId()).isEqualTo(C);
        assertThat(observedFacts.fromStopIndex()).isEqualTo(1);
        assertThat(observedFacts.toStopIndex()).isEqualTo(2);
        assertReadOnlySnapshot();
    }

    @Test
    void exactExpiryFreesPendingHoldWithoutCleanupOrAnyDatabaseWrite() {
        when(clock.instant()).thenReturn(EXPIRY.minusNanos(1000));
        assertThat(service.getSeatAvailability(TRIP, B, C).seats().get(3).status()).isEqualTo("UNAVAILABLE");
        Map<String, List<Map<String, Object>>> before = bookingRows();
        when(clock.instant()).thenReturn(EXPIRY);
        var response = service.getSeatAvailability(TRIP, B, C);
        assertMap(response, "FREE", "FREE", "UNAVAILABLE", "FREE", "FREE", "FREE");
        assertThat(response.seats()).extracting(SeatAvailabilityResponse.SeatState::expiresAt).containsOnlyNulls();
        assertThat(response.serverTime()).isEqualTo(EXPIRY);
        assertThat(jdbc.queryForObject("select status from booking where booking_id = ?", String.class, LIVE))
                .isEqualTo("Pending");
        assertThat(bookingRows()).isEqualTo(before);
    }

    @Test
    void oneTruncatedServerInstantDrivesRealTripFactsAndHoldValidity() {
        when(clock.instant()).thenReturn(EXPIRY.minusNanos(1), EXPIRY.plusSeconds(60));
        var response = service.getSeatAvailability(TRIP, B, C);
        assertThat(response.serverTime()).isEqualTo(Instant.parse("2026-11-19T00:09:59.999999Z"));
        assertThat(observedAsOf).isEqualTo(response.serverTime());
        assertThat(observedFacts.asOf()).isEqualTo(response.serverTime());
        assertThat(response.seats().get(3).status()).isEqualTo("UNAVAILABLE");
        assertThat(response.seats().get(3).expiresAt()).isEqualTo(EXPIRY);
        verify(clock, times(1)).instant();
    }

    @Test
    void snapshotSpansRealTripLayoutAndAllocationReadsAcrossConcurrentCommit() throws Exception {
        CountDownLatch factsRead = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);
        afterFacts = () -> {
            factsRead.countDown();
            await(committed, "Writer commit timed out");
        };
        var executor = Executors.newSingleThreadExecutor();
        var writer = executor.submit(() -> {
            await(factsRead, "Trip facts read timed out");
            try {
                return new TransactionTemplate(transactionManager).execute(tx -> {
                    jdbc.execute("set local statement_timeout = '10s'");
                    Integer pid = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
                    assertThat(jdbc.update("""
                            update trip_stop set planned_departure_time = planned_departure_time + interval '1 hour'
                            where trip_id = ? and stop_id = ?
                            """, TRIP, A)).isEqualTo(1);
                    assertThat(jdbc.update("update physical_seat set seat_code = 'A1-updated' where physical_seat_id = ?",
                            SEATS.getFirst())).isEqualTo(1);
                    assertThat(jdbc.update("""
                            update seat_allocation set released_at = timestamptz '2026-11-19T00:08:00Z'
                            where physical_seat_id = ?
                            """, SEATS.get(1))).isEqualTo(1);
                    return pid;
                });
            } finally {
                committed.countDown();
            }
        });
        try {
            var old = service.getSeatAvailability(TRIP, A, C);
            Integer writerPid = writer.get(15, TimeUnit.SECONDS);
            assertMap(old, "FREE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "FREE", "FREE");
            assertThat(old.layout().seats().getFirst().label()).isEqualTo("A1");
            assertThat(old.bookingClosesAt()).isEqualTo(CUTOFF);
            assertThat(old.serverTime()).isEqualTo(AS_OF);
            assertThat(observedAsOf).isEqualTo(AS_OF);
            assertThat(observedFacts.asOf()).isEqualTo(AS_OF);
            assertThat(writerPid).isNotNull().isNotEqualTo(tripConnection.get("pid"));
            assertReadOnlySnapshot();
            afterFacts = null;
            var fresh = service.getSeatAvailability(TRIP, A, C);
            assertMap(fresh, "FREE", "FREE", "UNAVAILABLE", "UNAVAILABLE", "FREE", "FREE");
            assertThat(fresh.layout().seats().getFirst().label()).isEqualTo("A1-updated");
            assertThat(fresh.bookingClosesAt()).isEqualTo(Instant.parse("2026-11-20T02:30:00Z"));
            assertThat(fresh.serverTime()).isEqualTo(AS_OF);
            assertReadOnlySnapshot();
        } finally {
            factsRead.countDown();
            committed.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).as("writer joined before cleanup").isTrue();
        }
    }

    @Test
    void allBlockedRealMapIs200NoStoreAndContainsNoBookingOrTravellerDataWithoutSecurityFilters() throws Exception {
        jdbc.execute("""
                insert into booking_seat (booking_seat_id, booking_id, physical_seat_id, traveller_name, fare_amount_vnd)
                select physical_seat_id, 'dddddddd-dddd-4ddd-8ddd-dddddddddd02', physical_seat_id,
                    'Synthetic private name', 250000 from physical_seat
                where physical_seat_id in ('77777777-7777-4777-8777-777777777777',
                    '77777777-7777-4777-8777-777777777780', '77777777-7777-4777-8777-777777777781')
                """);
        jdbc.execute("""
                insert into seat_allocation
                select s.booking_seat_id, s.booking_seat_id, b.booking_id, b.trip_id, s.physical_seat_id,
                    b.from_stop_index, b.to_stop_index, 'Confirmed', null, null
                from booking_seat s join booking b using (booking_id)
                where s.booking_seat_id = s.physical_seat_id
                """);
        var before = bookingRows();
        assertMap(service.getSeatAvailability(TRIP, A, C),
                "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE");
        var result = mvc.perform(get("/api/v1/trips/{tripId}/seat-availability", TRIP)
                        .param("fromStopId", A.toString()).param("toStopId", C.toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.seats.length()").value(6))
                .andExpect(jsonPath("$.seats[3].expiresAt").value("2026-11-19T00:10:00Z"))
                .andReturn();
        var body = json.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("tripId", "fromStopId", "toStopId", "serverTime", "bookingClosesAt", "layout", "seats");
        for (var seat : body.get("seats")) {
            assertThat(seat.properties()).extracting(Map.Entry::getKey)
                    .containsExactlyInAnyOrder("physicalSeatId", "status", "expiresAt");
            assertThat(seat.get("status").asText()).isEqualTo("UNAVAILABLE");
        }
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Synthetic", "dddddddd", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
                        "bookingId", "traveller", "passenger", "accountId");
        assertThat(bookingRows()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({"missing-trip,404,TRIP_NOT_FOUND", "missing-stop,422,STOP_NOT_SERVED",
            "reversed,422,INVALID_SEGMENT", "same-stop,422,INVALID_SEGMENT",
            "cancelled,409,TRIP_NOT_BOOKABLE", "missing-fare,409,ESSENTIAL_DATA_INCOMPLETE"})
    void realTripErrorsRetainTypedMappingAndSafeNoStoreBodyWithoutSecurityFilters(
            String scenario, int httpStatus, String code) throws Exception {
        UUID trip = TRIP;
        UUID from = A;
        UUID to = C;
        switch (scenario) {
            case "missing-trip" -> trip = UNKNOWN;
            case "missing-stop" -> from = UNKNOWN;
            case "reversed" -> { from = C; to = B; }
            case "same-stop" -> to = A;
            case "cancelled" -> jdbc.update("update trip set status = 'Cancelled' where trip_id = ?", TRIP);
            case "missing-fare" -> {
                jdbc.update("delete from leg_fare where trip_id = ? and from_stop_id = ?", TRIP, B);
                to = B;
            }
            default -> throw new IllegalArgumentException("Unknown scenario");
        }
        UUID requestedTrip = trip;
        UUID requestedFrom = from;
        UUID requestedTo = to;
        var before = bookingRows();
        assertThatThrownBy(() -> service.getSeatAvailability(requestedTrip, requestedFrom, requestedTo))
                .isInstanceOfSatisfying(TripQueryException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(TripQueryErrorCode.valueOf(code)));
        mvc.perform(get("/api/v1/trips/{tripId}/seat-availability", trip)
                        .param("fromStopId", from.toString()).param("toStopId", to.toString()))
                .andExpect(status().is(httpStatus)).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(code));
        assertThat(bookingRows()).isEqualTo(before);
    }

    @Test
    void firstStopCutoffAllowsOneMicrosecondBeforeAndRejectsExactOrAfterEvenForBc() {
        when(clock.instant()).thenReturn(CUTOFF.minusNanos(1000));
        assertThat(service.getSeatAvailability(TRIP, B, C).bookingClosesAt()).isEqualTo(CUTOFF);
        for (Instant now : List.of(CUTOFF, CUTOFF.plusNanos(1000))) {
            when(clock.instant()).thenReturn(now);
            assertThatThrownBy(() -> service.getSeatAvailability(TRIP, B, C))
                    .isInstanceOfSatisfying(TripQueryException.class,
                            failure -> assertThat(failure.getCode()).isEqualTo(TripQueryErrorCode.TRIP_NOT_BOOKABLE));
        }
    }

    @ParameterizedTest
    @CsvSource({"leg_fare", "seat_allocation"})
    void actualDatabaseReadFailureIsUnavailableAndNeverFalseFreeWithoutSecurityFilters(String table) throws Exception {
        var before = bookingRows();
        jdbc.execute("alter table " + table + " rename to task5_unavailable");
        try {
            assertThatThrownBy(() -> service.getSeatAvailability(TRIP, A, C))
                    .isInstanceOfSatisfying(TripQueryException.class, failure -> {
                        assertThat(failure.getCode()).isEqualTo(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE);
                        assertThat(failure.getCause()).isInstanceOf(org.springframework.dao.DataAccessException.class);
                    });
            var response = mvc.perform(get("/api/v1/trips/{tripId}/seat-availability", TRIP)
                            .param("fromStopId", A.toString()).param("toStopId", C.toString()))
                    .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.code").value("AVAILABILITY_UNAVAILABLE"))
                    .andExpect(jsonPath("$.seats").doesNotExist()).andReturn();
            assertThat(json.readTree(response.getResponse().getContentAsByteArray()).properties())
                    .extracting(Map.Entry::getKey).containsExactlyInAnyOrder("code", "message", "fieldErrors");
            assertThat(response.getResponse().getContentAsString())
                    .doesNotContain(table, "task5_unavailable", "SQL", "Exception", "Synthetic", "jdbc:");
        } finally {
            jdbc.execute("alter table task5_unavailable rename to " + table);
        }
        assertThat(bookingRows()).isEqualTo(before);
    }

    private Map<String, Object> connectionState() {
        return jdbc.queryForMap("""
                select pg_backend_pid() as pid, current_setting('transaction_isolation') as isolation,
                    current_setting('transaction_read_only') as read_only
                """);
    }

    private void assertReadOnlySnapshot() {
        assertThat(tripConnection).isNotNull().containsEntry("isolation", "repeatable read")
                .containsEntry("read_only", "on");
        assertThat(bookingConnection).isEqualTo(tripConnection);
    }

    private static void assertMap(SeatAvailabilityResponse response, String... states) {
        assertThat(response.layout().seats()).extracting(LayoutSeat::physicalSeatId).containsExactlyElementsOf(SEATS);
        assertThat(response.seats()).extracting(SeatAvailabilityResponse.SeatState::physicalSeatId)
                .containsExactlyElementsOf(SEATS);
        assertThat(response.seats()).extracting(SeatAvailabilityResponse.SeatState::status).containsExactly(states);
    }

    private Map<String, List<Map<String, Object>>> bookingRows() {
        return Map.of("booking", jdbc.queryForList("select * from booking order by booking_id"),
                "booking_seat", jdbc.queryForList("select * from booking_seat order by booking_seat_id"),
                "seat_allocation", jdbc.queryForList("select * from seat_allocation order by seat_allocation_id"));
    }

    private static void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message, failure);
        }
    }
}
