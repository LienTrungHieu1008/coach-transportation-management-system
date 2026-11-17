package vn.edu.team11.coach.booking.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.booking.domain.BookingStatus;
import vn.edu.team11.coach.booking.domain.SeatAllocation;
import vn.edu.team11.coach.booking.domain.Segment;
import vn.edu.team11.coach.booking.infrastructure.BookingRepository;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.SeatLayout;
import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripSeatFacts;
import vn.edu.team11.coach.trip.contract.TripStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeatAvailabilityApplicationServiceTests {
    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID FROM = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TO = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID FIRST = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID SECOND = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final Instant NOW = Instant.parse("2026-11-19T00:08:00.123456Z");
    private static final Instant CLOSES = Instant.parse("2026-11-19T01:00:00Z");
    private static final SeatLayout LAYOUT = new SeatLayout(UUID.fromString("66666666-6666-4666-8666-666666666666"),
            List.of(new LayoutSeat(SECOND, "B2", 1, 2, 3), new LayoutSeat(FIRST, "A1", 0, 0, 0)));
    private final BookingRepository repository = mock(BookingRepository.class);
    private final TripQueryPort port = mock(TripQueryPort.class);
    private final Clock clock = mock(Clock.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    private SeatAvailabilityApplicationService service;

    @BeforeEach
    void setUp() {
        beans.registerSingleton("tripQueryPort", port);
        service = new SeatAvailabilityApplicationService(beans.getBeanProvider(TripQueryPort.class), repository, clock);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-19T00:08:00.123456789Z"));
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenReturn(facts(TRIP, FROM, TO, NOW, TripStatus.Scheduled, CLOSES));
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(List.of());
    }

    @Test
    void mapsAllSeatsInVerifiedLayoutOrderUsingOneMicrosecondSnapshot() {
        var response = service.getSeatAvailability(TRIP, FROM, TO);
        assertThat(response.tripId()).isEqualTo(TRIP);
        assertThat(response.fromStopId()).isEqualTo(FROM);
        assertThat(response.toStopId()).isEqualTo(TO);
        assertThat(response.serverTime()).isEqualTo(NOW);
        assertThat(response.bookingClosesAt()).isEqualTo(CLOSES);
        assertThat(response.layout().id()).isEqualTo(LAYOUT.id());
        assertThat(response.layout().seats()).extracting("physicalSeatId").containsExactly(FIRST, SECOND);
        assertThat(response.layout().seats().get(1).label()).isEqualTo("B2");
        assertThat(response.layout().seats().get(1).deck()).isEqualTo(1);
        assertThat(response.layout().seats().get(1).row()).isEqualTo(2);
        assertThat(response.layout().seats().get(1).column()).isEqualTo(3);
        assertThat(response.seats()).extracting("physicalSeatId").containsExactly(FIRST, SECOND);
        assertThat(response.seats()).extracting("status").containsExactly("FREE", "FREE");
        assertThat(response.seats()).extracting("expiresAt").containsOnlyNulls();
        verify(clock).instant();
        verify(port).getSeatAvailabilityFacts(TRIP, FROM, TO, NOW);
        verify(repository, never()).expireDueBookings(any());
    }

    @Test
    void allBlockedIsNormalAndConfirmedOverridesHoldExpiry() {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(List.of(
                allocation(FIRST, BookingStatus.Confirmed, SeatAllocation.Kind.Confirmed, NOW.minusSeconds(30)),
                allocation(FIRST, BookingStatus.Pending, SeatAllocation.Kind.Hold, NOW.plusSeconds(10)),
                allocation(SECOND, BookingStatus.Pending, SeatAllocation.Kind.Hold, NOW.plusSeconds(40)),
                allocation(SECOND, BookingStatus.Pending, SeatAllocation.Kind.Hold, NOW.plusSeconds(20))));
        var response = service.getSeatAvailability(TRIP, FROM, TO);
        assertThat(response.seats()).extracting("status").containsExactly("UNAVAILABLE", "UNAVAILABLE");
        assertThat(response.seats().get(0).expiresAt()).isNull();
        assertThat(response.seats().get(1).expiresAt()).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    void expiredAndCancelledHoldsDoNotBlockWhenCleanupIsLate() {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(List.of(
                allocation(FIRST, BookingStatus.Pending, SeatAllocation.Kind.Hold, NOW),
                allocation(SECOND, BookingStatus.Cancelled, SeatAllocation.Kind.Hold, NOW.plusSeconds(20))));
        var response = service.getSeatAvailability(TRIP, FROM, TO);
        assertThat(response.seats()).extracting("status").containsExactly("FREE", "FREE");
        assertThat(response.seats()).extracting("expiresAt").containsOnlyNulls();
        verify(repository, never()).expireDueBookings(any());
    }

    @Test
    void missingRealPortFailsUnavailableBeforeReadingAllocations() {
        beans.destroySingleton("tripQueryPort");
        assertUnavailable();
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @MethodSource("unverifiedFacts")
    void rejectsUnverifiedFactsInsteadOfCalculatingFree(TripSeatFacts facts) {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenReturn(facts);
        assertUnavailable();
        verifyNoInteractions(repository);
    }

    static Stream<TripSeatFacts> unverifiedFacts() {
        return Stream.of(null,
                facts(TO, FROM, TO, NOW, TripStatus.Scheduled, CLOSES),
                facts(TRIP, TO, TO, NOW, TripStatus.Scheduled, CLOSES),
                facts(TRIP, FROM, FROM, NOW, TripStatus.Scheduled, CLOSES),
                facts(TRIP, FROM, TO, NOW.plusNanos(1), TripStatus.Scheduled, CLOSES),
                facts(TRIP, FROM, TO, NOW, TripStatus.Cancelled, CLOSES),
                facts(TRIP, FROM, TO, NOW, TripStatus.Scheduled, NOW));
    }

    @ParameterizedTest
    @MethodSource("invalidCandidates")
    void rejectsInconsistentReadRowsInsteadOfSilentlyDroppingThem(List<SeatAllocation> rows) {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(rows);
        assertUnavailable();
    }

    static Stream<List<SeatAllocation>> invalidCandidates() {
        return Stream.of(null, Arrays.asList((SeatAllocation) null),
                List.of(new SeatAllocation(TO, FIRST, new Segment(0, 2), BookingStatus.Confirmed, SeatAllocation.Kind.Confirmed, null, null)),
                List.of(allocation(TO, BookingStatus.Confirmed, SeatAllocation.Kind.Confirmed, null)),
                List.of(new SeatAllocation(TRIP, FIRST, new Segment(2, 3), BookingStatus.Confirmed, SeatAllocation.Kind.Confirmed, null, null)));
    }

    @Test
    void failedAllocationReadIsUnavailableWithoutFreeFallback() {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenThrow(new DataAccessResourceFailureException("SQL private"));
        assertUnavailable();
    }

    @Test
    void failedFactReadIsUnavailableWithoutFreeFallback() {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenThrow(new DataAccessResourceFailureException("SQL private"));
        assertUnavailable();
    }

    @Test
    void preservesTypedPortFailureForApiMapping() {
        var failure = new TripQueryException(TripQueryErrorCode.INVALID_SEGMENT, "private");
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenThrow(failure);
        assertThatThrownBy(() -> service.getSeatAvailability(TRIP, FROM, TO)).isSameAs(failure);
        verifyNoInteractions(repository);
    }

    @Test
    void unexpectedInternalFailureRemainsInternal() {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenThrow(new IllegalStateException("private"));
        assertThatThrownBy(() -> service.getSeatAvailability(TRIP, FROM, TO)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void declaresSharedReadOnlyRequiredRepeatableReadBoundary() throws Exception {
        var transaction = SeatAvailabilityApplicationService.class
                .getMethod("getSeatAvailability", UUID.class, UUID.class, UUID.class).getAnnotation(Transactional.class);
        assertThat(transaction).isNotNull();
        assertThat(transaction.readOnly()).isTrue();
        assertThat(transaction.propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(transaction.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
        assertThat(transaction.transactionManager()).isEmpty();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> service.getSeatAvailability(TRIP, FROM, TO))
                .isInstanceOfSatisfying(TripQueryException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE));
    }

    private static TripSeatFacts facts(UUID trip, UUID from, UUID to, Instant asOf, TripStatus status, Instant closes) {
        return new TripSeatFacts(trip, from, to, 0, 2, status, closes, LAYOUT, asOf);
    }

    private static SeatAllocation allocation(UUID seat, BookingStatus status, SeatAllocation.Kind kind, Instant expiry) {
        return new SeatAllocation(TRIP, seat, new Segment(0, 2), status, kind, expiry, null);
    }
}
