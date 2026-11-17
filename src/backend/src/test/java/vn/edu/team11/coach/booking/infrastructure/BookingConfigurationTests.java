package vn.edu.team11.coach.booking.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.FixedRateTask;

import vn.edu.team11.coach.booking.application.BookingExpiryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class BookingConfigurationTests {
    private final BookingExpiryService service = mock(BookingExpiryService.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(BookingConfiguration.class)
            .withBean(BookingExpiryService.class, () -> service)
            .withBean("taskScheduler", TaskScheduler.class, () -> mock(TaskScheduler.class));

    @Test
    void defaultsEnableFixedRateCleanupEveryFiveSecondsAndProvideNamedUtcClock() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(BookingExpiryScheduler.class);
            assertThat(context.getBean("bookingClock", Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertThat(tasks).singleElement().satisfies(task -> {
                assertThat(task.getTask()).isInstanceOf(FixedRateTask.class);
                assertThat(((FixedRateTask) task.getTask()).getIntervalDuration()).isEqualTo(Duration.ofSeconds(5));
            });
        });
    }

    @Test
    void disabledCleanupRegistersNoJobButStillProvidesClockAndValidatedInterval() {
        runner.withPropertyValues("booking.expiry.cleanup.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(BookingExpiryScheduler.class);
            assertThat(context).hasBean("bookingClock").hasBean("bookingExpiryCleanupInterval");
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
        });
    }

    @Test
    void environmentOverridesControlEnabledAndInterval() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("booking-test-env", Map.of(
                        "BOOKING_EXPIRY_CLEANUP_ENABLED", "false", "BOOKING_EXPIRY_CLEANUP_INTERVAL", "PT2S"))))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(BookingExpiryScheduler.class)
                            .hasBean("bookingExpiryCleanupInterval");
                    assertThat(context.getBean("bookingExpiryCleanupInterval", Duration.class)).isEqualTo(Duration.ofSeconds(2));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0.000000001S", "PT0.001S", "PT5S"})
    void positiveIntervalsUpToFiveSecondsSetTheActualFixedRate(String interval) {
        runner.withPropertyValues("booking.expiry.cleanup.interval=" + interval).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks())
                    .singleElement().satisfies(task -> assertThat(((FixedRateTask) task.getTask()).getIntervalDuration())
                            .isEqualTo(Duration.parse(interval)));
        });
    }

    @ParameterizedTest
    @CsvSource({"true,PT0S", "false,PT0S", "true,PT-1S", "false,PT-1S",
            "true,PT5.000000001S", "false,PT5.000000001S", "true,invalid", "false,invalid"})
    void invalidIntervalsPreventStartupEvenWhenCleanupIsDisabled(boolean enabled, String interval) {
        runner.withPropertyValues("booking.expiry.cleanup.enabled=" + enabled,
                "booking.expiry.cleanup.interval=" + interval).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void scheduledEntryCallsTheSeparateService() {
        new BookingExpiryScheduler(service).expireDueBookings();
        verify(service).expireDueBookings();
        verifyNoMoreInteractions(service);
    }
}
