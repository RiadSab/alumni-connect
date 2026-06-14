package dev.sabti.alumni_connect.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

// Exercises the token lifecycle the whole auth path relies on: a freshly generated token is VALID
// and round-trips the email; expired, tampered and malformed tokens are classified accordingly.
class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-signing";
    private static final String ISSUER = "alumni-connect";
    private static final String EMAIL = "user@example.com";

    private JwtUtil newJwtUtil(int expirationMs) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtIssuer", ISSUER);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", expirationMs);
        jwtUtil.init();
        return jwtUtil;
    }

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = newJwtUtil(3600_000);
    }

    @Test
    void freshToken_isValidAndRoundTripsEmail() {
        String token = jwtUtil.generateToken(EMAIL);

        assertThat(jwtUtil.checkToken(token)).isEqualTo(JwtUtil.TokenStatus.VALID);
        assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo(EMAIL);
    }

    @Test
    void expiredToken_isClassifiedExpired() {
        JwtUtil shortLived = newJwtUtil(-1000); // already expired when issued
        String token = shortLived.generateToken(EMAIL);

        assertThat(shortLived.checkToken(token)).isEqualTo(JwtUtil.TokenStatus.EXPIRED);
    }

    @Test
    void tamperedToken_isClassifiedInvalid() {
        String token = jwtUtil.generateToken(EMAIL);
        // Flip the last character of the signature so verification fails.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThat(jwtUtil.checkToken(tampered)).isEqualTo(JwtUtil.TokenStatus.INVALID);
    }

    @Test
    void malformedToken_isClassifiedInvalid() {
        assertThat(jwtUtil.checkToken("not-a-jwt")).isEqualTo(JwtUtil.TokenStatus.INVALID);
    }

    @Test
    void tokenFromAnotherSecret_isClassifiedInvalid() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "jwtSecret", "a-completely-different-secret-key-value-here");
        ReflectionTestUtils.setField(other, "jwtIssuer", ISSUER);
        ReflectionTestUtils.setField(other, "jwtExpirationMs", 3600_000);
        other.init();

        String foreignToken = other.generateToken(EMAIL);

        assertThat(jwtUtil.checkToken(foreignToken)).isEqualTo(JwtUtil.TokenStatus.INVALID);
    }
}
