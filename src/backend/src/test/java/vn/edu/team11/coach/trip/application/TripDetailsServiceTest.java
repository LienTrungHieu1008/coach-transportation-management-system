package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.team11.coach.trip.application.TripDetailsModels.Request;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;
import static vn.edu.team11.coach.trip.TripDetailsTestData.complete;

class TripDetailsServiceTest {
    private final TripDetailsRepository repository = mock(TripDetailsRepository.class);
    private final Clock clock = mock(Clock.class);
    private final TripDetailsService service = new TripDetailsService(repository, clock);
    @Test
    void capturesOneAsOfForAllFactsAndEligibility() {
        when(clock.instant()).thenReturn(NOW, CUTOFF);
        when(repository.load(DAY)).thenReturn(Optional.of(complete()));
        var result = service.details(new Request(DAY, A, C));
        assertThat(result.asOf()).isEqualTo(NOW);
        assertThat(result.reasons()).isEmpty();
        verify(clock, times(1)).instant();
        verify(repository).load(DAY);
        verifyNoMoreInteractions(repository);
    }
    @Test
    void onlyConfirmedAbsentTripIs404() {
        when(clock.instant()).thenReturn(NOW);
        when(repository.load(DAY)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.details(new Request(DAY, null, null)))
                .isInstanceOfSatisfying(TripDetailsException.class, e -> assertThat(e.status()).isEqualTo(404));
    }
    @Test
    void retrievalErrorIsNeverConvertedToNotFound() {
        when(clock.instant()).thenReturn(NOW);
        when(repository.load(DAY)).thenThrow(TripDetailsException.unavailable(new IllegalStateException()));
        assertThatThrownBy(() -> service.details(new Request(DAY, null, null)))
                .isInstanceOfSatisfying(TripDetailsException.class, e -> assertThat(e.status()).isEqualTo(503));
    }
    @Test
    void knownInvalidSegmentMaps422() {
        when(clock.instant()).thenReturn(NOW);
        when(repository.load(DAY)).thenReturn(Optional.of(complete()));
        when(repository.existingStops(List.of(C, A))).thenReturn(List.of(A, C));
        assertThatThrownBy(() -> service.details(new Request(DAY, C, A)))
                .isInstanceOfSatisfying(TripDetailsException.class, e -> assertThat(e.code()).isEqualTo("INVALID_SEGMENT"));
    }
}
