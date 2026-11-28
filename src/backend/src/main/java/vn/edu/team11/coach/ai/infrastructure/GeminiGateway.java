package vn.edu.team11.coach.ai.infrastructure;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.team11.coach.ai.application.AiAccessErrorCode;
import vn.edu.team11.coach.ai.application.AiProviderAccessException;
import vn.edu.team11.coach.ai.application.LlmGateway;
import vn.edu.team11.coach.ai.application.LlmGenerationResult;
import vn.edu.team11.coach.ai.domain.SafeAiInput;

public final class GeminiGateway implements LlmGateway {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiGateway.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GeminiGateway(
            AiProperties properties,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public LlmGenerationResult generateText(String systemInstruction, SafeAiInput userInput, Instant deadline) {
        Objects.requireNonNull(userInput, "userInput must not be null");
        Objects.requireNonNull(deadline, "deadline must not be null");

        if (properties.apiKey().isBlank()) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.MISSING_API_KEY,
                    "Gemini API key is not configured.",
                    null,
                    null,
                    0L,
                    null);
        }

        Duration remaining = Duration.between(clock.instant(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_TIMEOUT,
                    "Gemini request deadline expired before dispatch.",
                    null,
                    "DEADLINE_EXPIRED",
                    0L,
                    null);
        }

        Duration effectiveTimeout = remaining.compareTo(properties.timeout()) < 0
                ? remaining
                : properties.timeout();

        long startNanos = System.nanoTime();
        try {
            String requestBody = buildRequestBody(systemInstruction, userInput);
            HttpRequest request = HttpRequest.newBuilder(properties.generateContentUri())
                    .timeout(effectiveTimeout)
                    .header("Content-Type", "application/json")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latencyMs = elapsedMillis(startNanos);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseSuccessResponse(response.body(), latencyMs);
            }
            throw mapHttpFailure(response.statusCode(), response.body(), latencyMs);
        } catch (AiProviderAccessException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_TIMEOUT,
                    "Gemini API request timed out.",
                    null,
                    "HTTP_TIMEOUT",
                    elapsedMillis(startNanos),
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_UNAVAILABLE,
                    "Gemini API request was interrupted.",
                    null,
                    "INTERRUPTED",
                    elapsedMillis(startNanos),
                    ex);
        } catch (IOException ex) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_UNAVAILABLE,
                    "Gemini API connection failed.",
                    null,
                    ex.getClass().getSimpleName(),
                    elapsedMillis(startNanos),
                    ex);
        }
    }

    private String buildRequestBody(String systemInstruction, SafeAiInput userInput) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            payload.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))));
        }
        payload.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userInput.text())))));
        payload.put("generationConfig", Map.of("temperature", 0.1d));
        return objectMapper.writeValueAsString(payload);
    }

    private LlmGenerationResult parseSuccessResponse(String responseBody, long latencyMs) {
        if (responseBody == null || responseBody.isBlank()) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_INVALID_RESPONSE,
                    "Gemini API returned an empty response body.",
                    200,
                    "EMPTY_BODY",
                    latencyMs,
                    null);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String blockReason = root.path("promptFeedback").path("blockReason").asText("");
            if (!blockReason.isBlank()) {
                throw logAndBuildFailure(
                        AiAccessErrorCode.PROVIDER_INVALID_RESPONSE,
                        "Gemini API blocked the prompt.",
                        200,
                        blockReason,
                        latencyMs,
                        null);
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw logAndBuildFailure(
                        AiAccessErrorCode.PROVIDER_INVALID_RESPONSE,
                        "Gemini API returned no candidates.",
                        200,
                        "NO_CANDIDATES",
                        latencyMs,
                        null);
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText("UNKNOWN");
            JsonNode parts = firstCandidate.path("content").path("parts");
            StringBuilder textBuilder = new StringBuilder();
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    String partText = part.path("text").asText("");
                    if (!partText.isBlank()) {
                        if (!textBuilder.isEmpty()) {
                            textBuilder.append('\n');
                        }
                        textBuilder.append(partText);
                    }
                }
            }

            String generatedText = textBuilder.toString().trim();
            if (generatedText.isEmpty()) {
                throw logAndBuildFailure(
                        AiAccessErrorCode.PROVIDER_INVALID_RESPONSE,
                        "Gemini API candidate contained no text.",
                        200,
                        finishReason,
                        latencyMs,
                        null);
            }

            return new LlmGenerationResult(generatedText, properties.model(), finishReason, latencyMs);
        } catch (AiProviderAccessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw logAndBuildFailure(
                    AiAccessErrorCode.PROVIDER_INVALID_RESPONSE,
                    "Gemini API returned unreadable JSON.",
                    200,
                    "INVALID_JSON",
                    latencyMs,
                    ex);
        }
    }

    private AiProviderAccessException mapHttpFailure(int statusCode, String responseBody, long latencyMs) {
        ErrorMetadata metadata = parseErrorMetadata(responseBody);
        AiAccessErrorCode errorCode;
        String message;
        if (statusCode == 401 || statusCode == 403 || (statusCode == 400 && metadata.apiKeyInvalid())) {
            errorCode = AiAccessErrorCode.PROVIDER_AUTH_DENIED;
            message = "Gemini API rejected authentication or project access.";
        } else if (statusCode == 429) {
            errorCode = AiAccessErrorCode.PROVIDER_QUOTA_EXCEEDED;
            message = "Gemini API provider quota or rate limit exceeded.";
        } else if (statusCode == 400 || statusCode == 404) {
            errorCode = AiAccessErrorCode.PROVIDER_BAD_REQUEST;
            message = "Gemini API rejected the request or configured model.";
        } else {
            errorCode = AiAccessErrorCode.PROVIDER_UNAVAILABLE;
            message = "Gemini API returned an upstream server error.";
        }
        return logAndBuildFailure(
                errorCode,
                message,
                statusCode,
                metadata.upstreamStatus(),
                latencyMs,
                null);
    }

    private ErrorMetadata parseErrorMetadata(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ErrorMetadata(null, false);
        }
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody).path("error");
            String upstreamStatus = errorNode.path("status").asText("");
            boolean apiKeyInvalid = false;
            JsonNode details = errorNode.path("details");
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    if ("API_KEY_INVALID".equals(detail.path("reason").asText(""))) {
                        apiKeyInvalid = true;
                        break;
                    }
                }
            }
            return new ErrorMetadata(upstreamStatus.isBlank() ? null : upstreamStatus, apiKeyInvalid);
        } catch (IOException ex) {
            return new ErrorMetadata(null, false);
        }
    }

    private AiProviderAccessException logAndBuildFailure(
            AiAccessErrorCode errorCode,
            String message,
            Integer upstreamHttpStatus,
            String upstreamStatus,
            long latencyMs,
            Throwable cause) {
        AiProviderAccessException exception = new AiProviderAccessException(
                errorCode,
                message,
                upstreamHttpStatus,
                upstreamStatus,
                cause);
        LOG.warn(
                "Gemini access failed: errorCode={} fallbackReason={} model={} upstreamHttpStatus={} upstreamStatus={} latencyMs={}",
                exception.errorCode(),
                exception.fallbackReason(),
                properties.model(),
                exception.upstreamHttpStatus(),
                exception.upstreamStatus(),
                latencyMs);
        return exception;
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private record ErrorMetadata(String upstreamStatus, boolean apiKeyInvalid) {
    }
}
