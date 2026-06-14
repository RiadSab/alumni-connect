package dev.sabti.alumni_connect.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Verifies the per-request authentication decision: a present token only populates the
// SecurityContext when it is valid AND the account is still active. Bad tokens and inactive
// accounts leave the context empty and record a reason in JWT_ERROR_ATTRIBUTE for the entry point.
@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private MyUserDetailsService myUserDetailsService;
    @InjectMocks
    private JwtRequestFilter filter;

    private static final String EMAIL = "user@example.com";
    private static final String TOKEN = "any-token-value";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private UserDetails userWith(boolean enabled, boolean accountNonLocked) {
        return User.builder()
                .username(EMAIL)
                .password("hash")
                .roles("CANDIDATE")
                .disabled(!enabled)
                .accountLocked(!accountNonLocked)
                .build();
    }

    @Test
    void validTokenWithActiveAccount_populatesContext() throws Exception {
        when(jwtUtil.checkToken(TOKEN)).thenReturn(JwtUtil.TokenStatus.VALID);
        when(jwtUtil.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
        when(myUserDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(userWith(true, true));

        MockHttpServletRequest request = requestWithToken();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(EMAIL);
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isNull();
    }

    @Test
    void validTokenWithSuspendedAccount_isRefused() throws Exception {
        when(jwtUtil.checkToken(TOKEN)).thenReturn(JwtUtil.TokenStatus.VALID);
        when(jwtUtil.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
        // SUSPENDED maps to accountLocked = true in MyUserDetailsService.
        when(myUserDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(userWith(true, false));

        MockHttpServletRequest request = requestWithToken();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    void validTokenWithDisabledAccount_isRefused() throws Exception {
        when(jwtUtil.checkToken(TOKEN)).thenReturn(JwtUtil.TokenStatus.VALID);
        when(jwtUtil.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
        // Any non-ACTIVE/PENDING status maps to disabled = true in MyUserDetailsService.
        when(myUserDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(userWith(false, true));

        MockHttpServletRequest request = requestWithToken();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    void expiredToken_recordsTokenExpiredAndDoesNotAuthenticate() throws Exception {
        when(jwtUtil.checkToken(TOKEN)).thenReturn(JwtUtil.TokenStatus.EXPIRED);

        MockHttpServletRequest request = requestWithToken();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void invalidToken_recordsInvalidTokenAndDoesNotAuthenticate() throws Exception {
        when(jwtUtil.checkToken(TOKEN)).thenReturn(JwtUtil.TokenStatus.INVALID);

        MockHttpServletRequest request = requestWithToken();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void noToken_leavesContextEmptyWithoutAReason() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(); // no Authorization header

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtRequestFilter.JWT_ERROR_ATTRIBUTE)).isNull();
    }
}
