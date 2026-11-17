package vn.edu.team11.coach.booking.infrastructure;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;

import vn.edu.team11.coach.booking.application.BookingExpiryService;

public class BookingExpiryScheduler {
    private final BookingExpiryService service;

    public BookingExpiryScheduler(BookingExpiryService service) {
        this.service = service;
    }

    @Scheduled(fixedRateString = "#{@bookingExpiryCleanupInterval.toNanos()}", timeUnit = TimeUnit.NANOSECONDS)
    public void expireDueBookings() {
        service.expireDueBookings();
    }
}
