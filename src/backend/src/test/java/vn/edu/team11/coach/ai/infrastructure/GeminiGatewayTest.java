package vn.edu.team11.coach.ai.infrastructure;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.edu.team11.coach.ai.application.AiAccessErrorCode;
import vn.edu.team11.coach.ai.application.AiProviderAccessException;
import vn.edu.team11.coach.ai.application.LlmGenerationResult;
import vn.edu.team11.coach.ai.domain.FallbackReason;
import vn.edu.team11.coach.ai.domain.SafeAiInput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class GeminiGatewayTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-11-28T03:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String TEST_KEY = "unit-test-secret-key-do-not-leak";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private AtomicInteger requestCount;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void missingApiKeyFailsFastWithoutNetworkCall() {
        GeminiGateway gateway = newGateway("", Duration.ofSeconds(10));

        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText("sys", new SafeAiInput("Tìm chuyến Sài Gòn Đà Lạt"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.MISSING_API_KEY);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(requestCount.get()).isZero();
    }

    @Test
    void successfulGenerationSendsApiKeyInHeaderNotQueryAndParsesCandidateText() throws Exception {
        AtomicReference<String> capturedHeaderKey = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedQuery = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            capturedHeaderKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedQuery.set(exchange.getRequestURI().getQuery());
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              { "text": "{\\"origin\\":\\"TP.HCM\\",\\"destination\\":\\"Da Lat\\"}" }
                            ]
                          },
                          "finishReason": "STOP"
                        }
                      ]
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        LlmGenerationResult result = gateway.generateText(
                "Trích xuất tiêu chí JSON",
                new SafeAiInput("Tìm chuyến Sài Gòn đi Đà Lạt ngày mai"),
                FIXED_NOW.plusSeconds(10));

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(capturedHeaderKey.get()).isEqualTo(TEST_KEY);
        assertThat(capturedPath.get()).isEqualTo("/v1beta/models/gemini-2.5-flash:generateContent");
        assertThat(capturedQuery.get()).isNull();

        JsonNode sentJson = objectMapper.readTree(capturedBody.get());
        assertThat(sentJson.path("system_instruction").path("parts").get(0).path("text").asText())
                .isEqualTo("Trích xuất tiêu chí JSON");
        assertThat(sentJson.path("contents").get(0).path("parts").get(0).path("text").asText())
                .isEqualTo("Tìm chuyến Sài Gòn đi Đà Lạt ngày mai");

        assertThat(result.text()).isEqualTo("{\"origin\":\"TP.HCM\",\"destination\":\"Da Lat\"}");
        assertThat(result.model()).isEqualTo("gemini-2.5-flash");
        assertThat(result.finishReason()).isEqualTo("STOP");
    }

    @Test
    void forbiddenResponseMapsToProviderAuthDeniedWithoutLeakingApiKey() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 403, """
                    {
                      "error": {
                        "code": 403,
                        "message": "Caller does not have permission",
                        "status": "PERMISSION_DENIED"
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Hỏi chính sách hoàn vé"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_AUTH_DENIED);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(error.upstreamHttpStatus()).isEqualTo(403);
        assertThat(error.upstreamStatus()).isEqualTo("PERMISSION_DENIED");
        assertThat(error.getMessage()).doesNotContain(TEST_KEY);
    }

    @Test
    void invalidApiKey400ResponseMapsToProviderAuthDenied() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 400, """
                    {
                      "error": {
                        "code": 400,
                        "message": "API key not valid. Please pass a valid API key.",
                        "status": "INVALID_ARGUMENT",
                        "details": [
                          {
                            "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                            "reason": "API_KEY_INVALID"
                          }
                        ]
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Tìm chuyến đi Cần Thơ"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_AUTH_DENIED);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(error.upstreamHttpStatus()).isEqualTo(400);
        assertThat(error.upstreamStatus()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    void unknownModel404ResponseMapsToProviderBadRequest() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 404, """
                    {
                      "error": {
                        "code": 404,
                        "message": "models/nonexistent is not found",
                        "status": "NOT_FOUND"
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Tìm chuyến đi Nha Trang"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_BAD_REQUEST);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(error.upstreamHttpStatus()).isEqualTo(404);
        assertThat(error.upstreamStatus()).isEqualTo("NOT_FOUND");
    }

    @Test
    void rateLimited429ResponseMapsToProviderQuotaExceededAndLimitReachedFallback() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 429, """
                    {
                      "error": {
                        "code": 429,
                        "message": "Resource has been exhausted",
                        "status": "RESOURCE_EXHAUSTED"
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Tìm chuyến đi Vũng Tàu"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_QUOTA_EXCEEDED);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.LIMIT_REACHED);
        assertThat(error.upstreamHttpStatus()).isEqualTo(429);
        assertThat(error.upstreamStatus()).isEqualTo("RESOURCE_EXHAUSTED");
    }

    @Test
    void upstream503ResponseMapsToProviderUnavailable() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 503, """
                    {
                      "error": {
                        "code": 503,
                        "message": "The service is currently unavailable.",
                        "status": "UNAVAILABLE"
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException error = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Hỏi giờ khởi hành"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_UNAVAILABLE);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(error.upstreamHttpStatus()).isEqualTo(503);
    }

    @Test
    void expiredDeadlineAndSlowUpstreamFailWithTimeoutWithoutRetry() {
        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofMillis(150));

        AiProviderAccessException expiredError = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Hỏi tuyến xe"), FIXED_NOW),
                AiProviderAccessException.class);
        assertThat(expiredError).isNotNull();
        assertThat(expiredError.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_TIMEOUT);
        assertThat(expiredError.fallbackReason()).isEqualTo(FallbackReason.TIMEOUT);
        assertThat(requestCount.get()).isZero();

        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            try {
                Thread.sleep(600);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{}");
        });

        AiProviderAccessException slowError = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Hỏi tuyến xe"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);
        assertThat(slowError).isNotNull();
        assertThat(slowError.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_TIMEOUT);
        assertThat(slowError.fallbackReason()).isEqualTo(FallbackReason.TIMEOUT);
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void safetyBlockedOrEmptyCandidateMapsToProviderInvalidResponse() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 200, """
                    {
                      "promptFeedback": {
                        "blockReason": "SAFETY"
                      }
                    }
                    """);
        });

        GeminiGateway gateway = newGateway(TEST_KEY, Duration.ofSeconds(10));
        AiProviderAccessException blockedError = catchThrowableOfType(
                () -> gateway.generateText(null, new SafeAiInput("Hỏi quy định giữ chỗ"), FIXED_NOW.plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(blockedError).isNotNull();
        assertThat(blockedError.errorCode()).isEqualTo(AiAccessErrorCode.PROVIDER_INVALID_RESPONSE);
        assertThat(blockedError.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
        assertThat(blockedError.upstreamStatus()).isEqualTo("SAFETY");
    }

    private GeminiGateway newGateway(String apiKey, Duration timeout) {
        AiProperties properties = new AiProperties(
                apiKey,
                "gemini-2.5-flash",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                timeout,
                5,
                200,
                "");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new GeminiGateway(properties, client, objectMapper, FIXED_CLOCK);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
