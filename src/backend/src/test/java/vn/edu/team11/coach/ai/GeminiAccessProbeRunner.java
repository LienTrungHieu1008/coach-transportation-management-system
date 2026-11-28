package vn.edu.team11.coach.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.edu.team11.coach.ai.application.AiProviderAccessException;
import vn.edu.team11.coach.ai.application.LlmGenerationResult;
import vn.edu.team11.coach.ai.domain.InputSanitizer;
import vn.edu.team11.coach.ai.domain.SafeInputResult;
import vn.edu.team11.coach.ai.infrastructure.AiProperties;
import vn.edu.team11.coach.ai.infrastructure.GeminiGateway;

public final class GeminiAccessProbeRunner {
    private static final String SAMPLE_PROMPT = "Tìm chuyến xe khách từ TP.HCM đi Đà Lạt ngày mai";
    private static final String SYSTEM_INSTRUCTION =
            "Extract trip criteria as compact JSON with keys origin, destination, dateHint.";

    private GeminiAccessProbeRunner() {
    }

    public static void main(String[] args) {
        String apiKey = envOrDefault("GEMINI_API_KEY", "");
        String model = envOrDefault("GEMINI_MODEL", "gemini-2.5-flash");
        String baseUrl = envOrDefault("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com");
        Duration timeout = Duration.parse(envOrDefault("GEMINI_TIMEOUT", "PT10S"));
        int quotaPerMinute = Integer.parseInt(envOrDefault("AI_QUOTA_PER_MINUTE", "5"));
        int quotaDailyDemo = Integer.parseInt(envOrDefault("AI_QUOTA_DAILY_DEMO", "200"));
        String supportContact = envOrDefault("SUPPORT_CONTACT_CHANNEL", "");

        AiProperties properties = new AiProperties(
                apiKey,
                model,
                URI.create(baseUrl),
                timeout,
                quotaPerMinute,
                quotaDailyDemo,
                supportContact);

        InputSanitizer sanitizer = new InputSanitizer();
        SafeInputResult sanitized = sanitizer.sanitizeOrReject(SAMPLE_PROMPT);
        if (!(sanitized instanceof SafeInputResult.Accepted accepted)) {
            throw new IllegalStateException("Sample prompt unexpectedly rejected by InputSanitizer");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        GeminiGateway gateway = new GeminiGateway(properties, httpClient, new ObjectMapper(), Clock.systemUTC());

        Instant startedAt = Instant.now();
        try {
            LlmGenerationResult result = gateway.generateText(
                    SYSTEM_INSTRUCTION,
                    accepted.input(),
                    startedAt.plus(properties.timeout()));
            System.out.printf(
                    "PROBE_RESULT outcome=SUCCESS timestamp=%s baseUrl=%s model=%s apiKeyConfigured=%s finishReason=%s latencyMs=%d responseLength=%d%n",
                    startedAt,
                    properties.baseUrl(),
                    result.model(),
                    !properties.apiKey().isBlank(),
                    result.finishReason(),
                    result.latencyMs(),
                    result.text().length());
        } catch (AiProviderAccessException ex) {
            System.out.printf(
                    "PROBE_RESULT outcome=ACCESS_ERROR timestamp=%s baseUrl=%s model=%s apiKeyConfigured=%s errorCode=%s fallbackReason=%s upstreamHttpStatus=%s upstreamStatus=%s message=\"%s\"%n",
                    startedAt,
                    properties.baseUrl(),
                    properties.model(),
                    !properties.apiKey().isBlank(),
                    ex.errorCode(),
                    ex.fallbackReason(),
                    ex.upstreamHttpStatus(),
                    ex.upstreamStatus(),
                    ex.getMessage());
        }
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
