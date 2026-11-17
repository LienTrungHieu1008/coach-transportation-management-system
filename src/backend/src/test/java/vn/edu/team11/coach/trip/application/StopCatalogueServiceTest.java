package vn.edu.team11.coach.trip.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StopCatalogueServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-17T08:00:00Z");
    private final StopCatalogueRepository repository = mock(StopCatalogueRepository.class);

    @Test
    void returnsRepositoryOrderAndCapturesServerTimeOnceWithImmutableItems() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        var stop = new ServedStop(UUID.fromString("22222222-2222-4222-8222-222222222222"), "Stop A");
        var source = new ArrayList<>(List.of(stop));
        when(repository.findServedStops()).thenReturn(source);

        var result = new StopCatalogueService(repository, clock).listStops();
        source.clear();

        assertThat(result.items()).containsExactly(stop);
        assertThat(result.serverTime()).isEqualTo(NOW);
        assertThatThrownBy(() -> result.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        verify(clock, times(1)).instant();
        verify(repository, times(1)).findServedStops();
    }

    @Test
    void emptyCatalogueIsSuccessfulWithServerTime() {
        when(repository.findServedStops()).thenReturn(List.of());
        var result = new StopCatalogueService(repository, Clock.fixed(NOW, ZoneOffset.UTC)).listStops();
        assertThat(result.items()).isEmpty();
        assertThat(result.serverTime()).isEqualTo(NOW);
    }

    @Test
    void retrievalFailureIsPropagatedRatherThanFabricatedAsEmptyCatalogue() {
        var failure = new StopCatalogueUnavailableException(new IllegalStateException("database unavailable"));
        when(repository.findServedStops()).thenThrow(failure);
        var service = new StopCatalogueService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(service::listStops).isSameAs(failure);
    }
}
