package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import vn.edu.team11.coach.trip.application.TripSearchModels.ReadData;
import vn.edu.team11.coach.trip.application.TripSearchModels.Request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

class TripSearchServiceTest {
    private final TripSearchRepository repository = mock(TripSearchRepository.class);

    @Test
    void oneAsOfIsUsedForRepositoryAndResponse() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        var request = new Request(A, C, DATE, 0, 20);
        when(repository.load(request, NOW)).thenReturn(new ReadData(true, true, true, List.of(complete())));
        var result = new TripSearchService(repository, clock).search(request);
        assertThat(result.serverTime()).isEqualTo(NOW);
        assertThat(result.items().getFirst().fareAmountVnd()).isEqualTo(250_000);
        verify(clock, times(1)).instant();
        verify(repository).load(request, NOW);
    }

    @Test
    void pastDateUsesVietnamDayEvenWhenUtcIsPreviousDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T17:00:00Z"), ZoneOffset.UTC);
        assertCode(new TripSearchService(repository, clock), new Request(A, B, DATE, 0, 20), "PAST_BOARDING_DATE");
        verifyNoInteractions(repository);
    }

    @Test
    void unservedAndReversedSegmentAreBusinessErrors() {
        var service = new TripSearchService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new Request(A, B, DATE, 0, 20);
        when(repository.load(any(), any())).thenReturn(new ReadData(false, true, false, List.of()));
        assertCode(service, request, "STOP_NOT_SERVED");
        when(repository.load(any(), any())).thenReturn(new ReadData(true, true, false, List.of()));
        assertCode(service, request, "INVALID_SEGMENT");
    }

    @Test
    void largePageOffsetDoesNotOverflowAndKeepsTrueTotals() {
        when(repository.load(any(), any())).thenReturn(new ReadData(true, true, true, List.of(complete())));
        var result = new TripSearchService(repository, Clock.fixed(NOW, ZoneOffset.UTC))
                .search(new Request(A, B, DATE, Integer.MAX_VALUE, 100));
        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void emptyResultHasZeroPagesAndRetrievalFailuresAreNotReplacedByEmptySuccess() {
        var service = new TripSearchService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new Request(A, B, DATE, 0, 20);
        when(repository.load(any(), any())).thenReturn(new ReadData(true, true, true, List.of()));
        assertThat(service.search(request).totalPages()).isZero();
        var failure = TripSearchException.unavailable(new IllegalStateException("DB failure"));
        when(repository.load(any(), any())).thenThrow(failure);
        assertThatThrownBy(() -> service.search(request)).isSameAs(failure);
    }

    private void assertCode(TripSearchService service, Request request, String code) {
        assertThatThrownBy(() -> service.search(request)).isInstanceOfSatisfying(TripSearchException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
