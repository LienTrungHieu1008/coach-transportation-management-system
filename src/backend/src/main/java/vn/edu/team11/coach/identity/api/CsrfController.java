package vn.edu.team11.coach.identity.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/api/session/csrf")
    ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        // Resolving the deferred token creates/persists its server-side JDBC session.
        var body = new CsrfResponse(token.getHeaderName(), token.getToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    record CsrfResponse(String headerName, String token) {
    }
}
