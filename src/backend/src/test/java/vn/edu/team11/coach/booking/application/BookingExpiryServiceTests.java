package vn.edu.team11.coach.booking.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import vn.edu.team11.coach.booking.infrastructure.BookingRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BookingExpiryServiceTests {
    @Test
    void readsClockOnceTruncatesToMicrosecondsAndReturnsChangedBookingCount() {
        BookingRepository repository = mock(BookingRepository.class);
        Clock clock = mock(Clock.class);
        Instant now = Instant.parse("2026-11-19T00:10:00.123456789Z");
        Instant asOf = Instant.parse("2026-11-19T00:10:00.123456Z");
        when(clock.instant()).thenReturn(now);
        when(repository.expireDueBookings(asOf)).thenReturn(3);

        assertThat(new BookingExpiryService(repository, clock).expireDueBookings()).isEqualTo(3);
        verify(clock).instant();
        verify(repository).expireDueBookings(asOf);
        verifyNoMoreInteractions(clock, repository);
    }

    @Test
    void propagatesWriteFailure() {
        BookingRepository repository = mock(BookingRepository.class);
        Instant asOf = Instant.parse("2026-11-19T00:10:00Z");
        var failure = new IllegalStateException("Write failed");
        when(repository.expireDueBookings(asOf)).thenThrow(failure);
        Clock clock = Clock.fixed(asOf, ZoneOffset.UTC);

        assertThatThrownBy(() -> new BookingExpiryService(repository, clock).expireDueBookings()).isSameAs(failure);
    }
}
