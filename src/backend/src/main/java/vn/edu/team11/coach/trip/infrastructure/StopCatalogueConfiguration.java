package vn.edu.team11.coach.trip.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StopCatalogueConfiguration {
    @Bean("stopCatalogueClock")
    Clock stopCatalogueClock() {
        return Clock.systemUTC();
    }
}
