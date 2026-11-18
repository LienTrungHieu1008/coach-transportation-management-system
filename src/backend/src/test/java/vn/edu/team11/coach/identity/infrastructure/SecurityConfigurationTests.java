package vn.edu.team11.coach.identity.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.team11.coach.identity.api.CsrfController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CsrfController.class, SecurityConfigurationTests.SecurityProbeController.class})
@Import({SecurityConfiguration.class, SecurityConfigurationTests.SecurityProbeController.class})
class SecurityConfigurationTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousClientCanObtainCsrfTokenBeforeLogin() throws Exception {
        mvc.perform(get("/api/session/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token", not(emptyString())));
    }

    @Test
    void protectedApiReturnsJsonUnauthorizedInsteadOfLoginPage() throws Exception {
        mvc.perform(get("/api/private/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void authenticatedWriteWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/private/security-probe").with(user("passenger")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void csrfTokenDoesNotGrantAuthentication() throws Exception {
        mvc.perform(post("/api/private/security-probe").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedWriteWithValidCsrfReachesController() throws Exception {
        mvc.perform(post("/api/private/security-probe").with(user("passenger")).with(csrf()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/trips/", "/api/v1/trips/11111111-1111-4111-8111-111111111111",
            "/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability/private",
            "/api/v1/stops/", "/api/v1/stops/other", "/api/v1/other"})
    void publicMatchersDoNotExposeOtherPaths(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/v1/stops", "PUT,/api/v1/stops", "PATCH,/api/v1/stops", "DELETE,/api/v1/stops",
            "POST,/api/v1/trips", "PUT,/api/v1/trips", "PATCH,/api/v1/trips", "DELETE,/api/v1/trips",
            "POST,/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability",
            "PUT,/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability",
            "PATCH,/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability",
            "DELETE,/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability"})
    void publicPathWritesStillRequireCsrfAndAuthentication(String method, String path) throws Exception {
        var verb = HttpMethod.valueOf(method);
        mvc.perform(request(verb, path).with(user("passenger")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(request(verb, path).with(user("passenger"))
                        .with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(request(verb, path).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // This route exists only in the test slice; production exposes no test account/write API.
    @RestController
    static class SecurityProbeController {
        @GetMapping("/api/private/security-probe")
        String read() {
            return "read";
        }

        @PostMapping("/api/private/security-probe")
        String write() {
            return "written";
        }
    }
}
