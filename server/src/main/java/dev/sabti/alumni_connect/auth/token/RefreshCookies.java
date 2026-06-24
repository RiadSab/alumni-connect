package dev.sabti.alumni_connect.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Builds the httpOnly refresh-token cookie. Scoped to /api/auth so it's only sent to the auth
// endpoints; SameSite=Lax keeps it off cross-site requests; Secure is off only for local http dev.
@Component
public class RefreshCookies {
    public static final String COOKIE_NAME = "refreshToken";
    private static final Duration TTL = Duration.ofDays(RefreshTokenService.REFRESH_TTL_DAYS);

    @Value("${app.auth.cookie-secure:true}")
    private boolean secure;

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(TTL).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/auth");
    }
}
