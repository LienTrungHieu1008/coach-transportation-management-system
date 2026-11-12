package vn.edu.team11.coach.identity.infrastructure;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json) throws Exception {
        var tokens = new HttpSessionCsrfTokenRepository();
        tokens.setHeaderName("X-CSRF-TOKEN");

        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/session/csrf").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf.csrfTokenRepository(tokens))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> writeError(
                                json, response, 401, "UNAUTHENTICATED", "Authentication is required."))
                        .accessDeniedHandler((request, response, error) -> writeError(
                                json, response, 403, "ACCESS_DENIED", "Request is not permitted.")));
        return http.build();
    }

    private void writeError(ObjectMapper json, HttpServletResponse response, int status,
                            String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of(
                "code", code, "message", message, "fieldErrors", List.of()));
    }
}
