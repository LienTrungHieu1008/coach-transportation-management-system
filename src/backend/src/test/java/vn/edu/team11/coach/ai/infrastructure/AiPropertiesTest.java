package vn.edu.team11.coach.ai.infrastructure;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPropertiesTest {

    @Test
    void allowsBlankApiKeyAndBlankSupportContactChannelWithDefaults() {
        AiProperties properties = new AiProperties(
                "  ",
                "gemini-2.5-flash",
                URI.create("https://generativelanguage.googleapis.com/"),
                Duration.ofSeconds(10),
                5,
                200,
                null);

        assertThat(properties.apiKey()).isEmpty();
        assertThat(properties.supportContactChannel()).isEmpty();
        assertThat(properties.generateContentUri().toString())
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
    }

    @Test
    void rejectsTimeoutOutsideTenSecondBudget() {
        assertThatThrownBy(() -> new AiProperties(
                "",
                "gemini-2.5-flash",
                URI.create("https://generativelanguage.googleapis.com"),
                Duration.ofSeconds(11),
                5,
                200,
                ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AiProperties(
                "",
                "gemini-2.5-flash",
                URI.create("https://generativelanguage.googleapis.com"),
                Duration.ZERO,
                5,
                200,
                ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidModelNameOrNonHttpBaseUrlOrNonPositiveQuota() {
        assertThatThrownBy(() -> new AiProperties(
                "",
                "../secret",
                URI.create("https://generativelanguage.googleapis.com"),
                Duration.ofSeconds(10),
                5,
                200,
                ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AiProperties(
                "",
                "gemini-2.5-flash",
                URI.create("ftp://generativelanguage.googleapis.com"),
                Duration.ofSeconds(10),
                5,
                200,
                ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AiProperties(
                "",
                "gemini-2.5-flash",
                URI.create("https://generativelanguage.googleapis.com"),
                Duration.ofSeconds(10),
                0,
                200,
                ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
