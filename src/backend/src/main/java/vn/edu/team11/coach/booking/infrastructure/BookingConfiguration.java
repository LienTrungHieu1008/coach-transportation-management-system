package vn.edu.team11.coach.booking.infrastructure;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import vn.edu.team11.coach.booking.application.BookingExpiryService;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class BookingConfiguration {
    @Bean
    Clock bookingClock() {
        return Clock.systemUTC();
    }

    @Bean
    Duration bookingExpiryCleanupInterval(@Value("${booking.expiry.cleanup.interval:PT5S}") String interval) {
        Duration duration = Duration.parse(interval);
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("Booking expiry cleanup interval must be positive and at most PT5S");
        }
        return duration;
    }

    @Bean
    @ConditionalOnProperty(name = "booking.expiry.cleanup.enabled", havingValue = "true", matchIfMissing = true)
    BookingExpiryScheduler bookingExpiryScheduler(BookingExpiryService service) {
        return new BookingExpiryScheduler(service);
    }
}
