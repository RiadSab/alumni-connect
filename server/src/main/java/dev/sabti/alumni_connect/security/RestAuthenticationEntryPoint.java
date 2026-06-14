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

// Writes a 401 ApiError body when authentication fails in the filter chain (before any controller,
// so GlobalExceptionHandler can't). The code says why: TOKEN_EXPIRED / INVALID_TOKEN (from
// JwtRequestFilter) or AUTH_REQUIRED when no token was sent.
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
