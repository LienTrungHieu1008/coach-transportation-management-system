package vn.edu.team11.coach.trip.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.team11.coach.identity.infrastructure.SecurityConfiguration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.DAY;

@WebMvcTest(TripDetailsSecurityMatchersTest.ProbeController.class)
@Import({SecurityConfiguration.class, TripDetailsSecurityMatchersTest.ProbeController.class})
class TripDetailsSecurityMatchersTest {
    @Autowired private MockMvc mvc;
    private static final String PATH = "/api/v1/trips/" + DAY;

    @Test
    void onlyGetDetailsIsPublic() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(content().string("details probe"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void realWriteProbeRequiresBothAuthenticationAndCsrf(String method) throws Exception {
        var verb = HttpMethod.valueOf(method);
        mvc.perform(request(verb, PATH)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(request(verb, PATH).with(csrf())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(request(verb, PATH).with(user("passenger"))).andExpect(status().isForbidden());
        mvc.perform(request(verb, PATH).with(user("passenger")).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        // The same route exists and is reachable with both credentials; no 404/405 evidence.
        mvc.perform(request(verb, PATH).with(user("passenger")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string("write probe"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"/seat-availability", "/operations", "/nested/deeper"})
    void nestedProbeIsProtectedEvenThoughControllerExists(String suffix) throws Exception {
        mvc.perform(get(PATH + suffix)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get(PATH + suffix).with(user("passenger"))).andExpect(status().isOk())
                .andExpect(content().string("protected probe"));
    }
    @Test
    void unrelatedProtectedProbeRemainsProtected() throws Exception {
        mvc.perform(get("/api/private/p4-security-probe")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/private/p4-security-probe").with(user("passenger")))
                .andExpect(status().isOk()).andExpect(content().string("protected probe"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"HEAD", "OPTIONS"})
    void otherMethodsAreNotGrantedAnonymousAccess(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), PATH)).andExpect(status().isUnauthorized());
    }

    // Test slice only: deliberately existing endpoints prove authorization runs before MVC.
    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/trips/{tripId}")
        String details() { return "details probe"; }
        @RequestMapping(path = "/api/v1/trips/{tripId}", method = {
                RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
        String write() { return "write probe"; }
        @GetMapping({"/api/v1/trips/{tripId}/seat-availability", "/api/v1/trips/{tripId}/operations",
                "/api/v1/trips/{tripId}/nested/deeper", "/api/private/p4-security-probe"})
        String protectedRead() { return "protected probe"; }
    }
}
