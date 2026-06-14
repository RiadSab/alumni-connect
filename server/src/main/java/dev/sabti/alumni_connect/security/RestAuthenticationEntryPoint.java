package dev.sabti.alumni_connect.security;

import tools.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Authentication failed at the filter chain (no usable credentials), so the request never reached a
// controller and GlobalExceptionHandler can't see it. This runs at the servlet level instead, and
// writes the same ApiError body by hand with Jackson so a 401 looks like every other error to the
// frontend. The injected ObjectMapper is Spring's MVC one (JSR-310 registered), so Instant serialises
// the same way it does everywhere else.
//
// The code distinguishes the three 401 reasons the HTTP status alone can't: a token that simply
// expired (TOKEN_EXPIRED) vs a malformed/forged one (INVALID_TOKEN) vs no token at all
// (AUTH_REQUIRED). JwtRequestFilter records the first two on the request; their absence means none
// was sent.
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE);
        String message;
        if ("TOKEN_EXPIRED".equals(code)) {
            message = "Your session has expired, please log in again";
        } else if ("INVALID_TOKEN".equals(code)) {
            message = "Invalid authentication token";
        } else {
            code = "AUTH_REQUIRED";
            message = "Authentication is required to access this resource";
        }

        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED, message, code);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
