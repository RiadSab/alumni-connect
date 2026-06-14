package dev.sabti.alumni_connect.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

// Verifies the 401 body the entry point writes for each reason JwtRequestFilter records, plus the
// fallback when no reason is present (no token was sent).
class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
    private final AuthenticationException authException =
            new InsufficientAuthenticationException("denied");

    private JsonNode commenceWithReason(String reason) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (reason != null) {
            request.setAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE, reason);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, authException);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        return objectMapper.readTree(response.getContentAsString());
    }

    @Test
    void accountDisabledReason_returnsAccountDisabledCode() throws Exception {
        JsonNode body = commenceWithReason("ACCOUNT_DISABLED");

        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("code").asString()).isEqualTo("ACCOUNT_DISABLED");
        assertThat(body.get("message").asString()).isNotBlank();
    }

    @Test
    void expiredReason_returnsTokenExpiredCode() throws Exception {
        JsonNode body = commenceWithReason("TOKEN_EXPIRED");

        assertThat(body.get("code").asString()).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void invalidReason_returnsInvalidTokenCode() throws Exception {
        JsonNode body = commenceWithReason("INVALID_TOKEN");

        assertThat(body.get("code").asString()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void noReason_fallsBackToAuthRequired() throws Exception {
        JsonNode body = commenceWithReason(null);

        assertThat(body.get("code").asString()).isEqualTo("AUTH_REQUIRED");
    }
}
