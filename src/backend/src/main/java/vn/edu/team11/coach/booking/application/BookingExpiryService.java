package vn.edu.team11.coach.booking.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.booking.infrastructure.BookingRepository;

@Service
public class BookingExpiryService {
    private final BookingRepository repository;
    private final Clock clock;

    public BookingExpiryService(BookingRepository repository, @Qualifier("bookingClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public int expireDueBookings() {
        return repository.expireDueBookings(clock.instant().truncatedTo(ChronoUnit.MICROS));
    }
}
