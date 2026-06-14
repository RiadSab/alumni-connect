package dev.sabti.alumni_connect.security;

import tools.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// The caller IS authenticated but lacks the required role (e.g. a company user hitting a
// CANDIDATE-only endpoint). Spring Security rejects this at the filter chain, before any controller,
// so GlobalExceptionHandler never sees it — this writes the ApiError body by hand instead, mirroring
// RestAuthenticationEntryPoint. ACCESS_DENIED is the security 403, kept distinct from a domain
// ForbiddenException (whose code is FORBIDDEN) so the frontend can tell "you're not allowed here at
// all" from "the action itself was refused".
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource", "ACCESS_DENIED");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
