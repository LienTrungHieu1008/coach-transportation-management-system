package vn.edu.team11.coach.ai.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import vn.edu.team11.coach.ai.application.AiAccessErrorCode;
import vn.edu.team11.coach.ai.application.AiProviderAccessException;
import vn.edu.team11.coach.ai.application.LlmGateway;
import vn.edu.team11.coach.ai.domain.FallbackReason;
import vn.edu.team11.coach.ai.domain.InputSanitizer;
import vn.edu.team11.coach.ai.domain.SafeInputResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeminiGatewayContextIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Autowired
    private LlmGateway llmGateway;

    @Test
    void contextStartsWithoutApiKeyAndFailsExplicitlyWithMissingApiKey() throws Exception {
        assertThat(aiProperties.apiKey()).isEmpty();
        assertThat(aiProperties.model()).isEqualTo("gemini-2.5-flash");
        assertThat(aiProperties.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(aiProperties.quotaPerMinute()).isEqualTo(5);
        assertThat(aiProperties.quotaDailyDemo()).isEqualTo(200);
        assertThat(aiProperties.supportContactChannel()).isEmpty();

        HttpResponse<String> health = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);

        SafeInputResult sanitized = inputSanitizer.sanitizeOrReject("Tìm chuyến xe đi Đà Lạt ngày mai");
        assertThat(sanitized).isInstanceOf(SafeInputResult.Accepted.class);
        var accepted = (SafeInputResult.Accepted) sanitized;

        AiProviderAccessException error = catchThrowableOfType(
                () -> llmGateway.generateText(
                        "Trả lời ngắn gọn bằng JSON.",
                        accepted.input(),
                        Instant.now().plusSeconds(10)),
                AiProviderAccessException.class);

        assertThat(error).isNotNull();
        assertThat(error.errorCode()).isEqualTo(AiAccessErrorCode.MISSING_API_KEY);
        assertThat(error.fallbackReason()).isEqualTo(FallbackReason.SERVICE_UNAVAILABLE);
    }
}
