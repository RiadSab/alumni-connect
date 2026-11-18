package dev.sabti.alumni_connect.rate_limiting;

import dev.sabti.alumni_connect.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RateLimitResponseWriter {
    private final ObjectMapper objectMapper;

    public RateLimitResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write429(HttpServletResponse response, RateLimitDecision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(response.getWriter(), ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later.", "RATE_LIMITED"));
    }
}
