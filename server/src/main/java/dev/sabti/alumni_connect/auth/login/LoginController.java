package dev.sabti.alumni_connect.auth.login;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.token.RefreshCookies;
import dev.sabti.alumni_connect.auth.token.RefreshTokenService;
import dev.sabti.alumni_connect.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookies refreshCookies;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return loginService.login(dto)
                .map(result -> ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookies.create(result.refreshToken()).toString())
                        .body(result.response()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // Exchanges the refresh-token cookie for a new access token (and rotates the cookie). 401 +
    // cleared cookie when the token is missing, expired, or reused — the client then logs out.
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
            @CookieValue(name = RefreshCookies.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return refreshTokenService.rotate(refreshToken)
                .map(rotation -> {
                    User user = rotation.user();
                    String access = jwtUtil.generateToken(user.getEmail());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, refreshCookies.create(rotation.rawToken()).toString())
                            .body(LoginResponseDTO.of(access, user));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                        .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookies.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }
}
