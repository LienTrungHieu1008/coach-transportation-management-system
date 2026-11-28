package vn.edu.team11.coach.ai.infrastructure;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public record AiProperties(
        String apiKey,
        String model,
        URI baseUrl,
        Duration timeout,
        int quotaPerMinute,
        int quotaDailyDemo,
        String supportContactChannel) {

    public static final Duration MAX_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    public AiProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        if (model == null || model.isBlank() || !MODEL_NAME_PATTERN.matcher(model.trim()).matches()) {
            throw new IllegalArgumentException("AI Gemini model name must match [A-Za-z0-9._-]+");
        }
        model = model.trim();

        Objects.requireNonNull(baseUrl, "AI Gemini baseUrl must not be null");
        String scheme = baseUrl.getScheme();
        if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || baseUrl.getHost() == null || baseUrl.getHost().isBlank()) {
            throw new IllegalArgumentException("AI Gemini baseUrl must be a valid http or https URI with host");
        }

        Objects.requireNonNull(timeout, "AI Gemini timeout must not be null");
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException("AI Gemini timeout must be positive and at most PT10S");
        }

        if (quotaPerMinute <= 0) {
            throw new IllegalArgumentException("AI quotaPerMinute must be positive");
        }
        if (quotaDailyDemo <= 0) {
            throw new IllegalArgumentException("AI quotaDailyDemo must be positive");
        }

        supportContactChannel = supportContactChannel == null ? "" : supportContactChannel.trim();
    }

    public URI generateContentUri() {
        String normalizedBase = baseUrl.toString();
        while (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        return URI.create(normalizedBase + "/v1beta/models/" + model + ":generateContent");
    }
}
