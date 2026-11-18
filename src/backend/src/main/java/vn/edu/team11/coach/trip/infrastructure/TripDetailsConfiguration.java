package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TripDetailsConfiguration {
    @Bean("tripDetailsClock")
    Clock tripDetailsClock() { return Clock.systemUTC(); }
}
