package vn.edu.team11.coach.ai.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.edu.team11.coach.ai.application.LlmGateway;
import vn.edu.team11.coach.ai.domain.InputSanitizer;

@Configuration(proxyBeanMethods = false)
public class AiConfiguration {

    @Bean("aiClock")
    Clock aiClock() {
        return Clock.systemUTC();
    }

    @Bean
    AiProperties aiProperties(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-2.5-flash}") String model,
            @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.gemini.timeout:PT10S}") String timeout,
            @Value("${ai.quota.per-minute:5}") int quotaPerMinute,
            @Value("${ai.quota.daily-demo:200}") int quotaDailyDemo,
            @Value("${ai.support.contact-channel:}") String supportContactChannel) {
        return new AiProperties(
                apiKey,
                model,
                URI.create(baseUrl.trim()),
                Duration.parse(timeout.trim()),
                quotaPerMinute,
                quotaDailyDemo,
                supportContactChannel);
    }

    @Bean
    InputSanitizer inputSanitizer() {
        return new InputSanitizer();
    }

    @Bean("aiHttpClient")
    HttpClient aiHttpClient(AiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    LlmGateway llmGateway(
            AiProperties properties,
            @Qualifier("aiHttpClient") HttpClient httpClient,
            ObjectMapper objectMapper,
            @Qualifier("aiClock") Clock clock) {
        return new GeminiGateway(properties, httpClient, objectMapper, clock);
    }
}
