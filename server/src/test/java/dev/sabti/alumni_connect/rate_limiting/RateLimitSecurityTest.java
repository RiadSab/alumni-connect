package dev.sabti.alumni_connect.rate_limiting;

import dev.sabti.alumni_connect.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RateLimitSecurityTest.ProbeController.class, properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.policies.source-login.capacity=1",
        "app.rate-limit.policies.source-login.refill-period=1h",
        "app.rate-limit.policies.source-registration.capacity=1",
        "app.rate-limit.policies.source-registration.refill-period=1h",
        "app.rate-limit.policies.user-upload.capacity=1",
        "app.rate-limit.policies.user-upload.refill-period=1h"
})
@Import({SecurityConfig.class, CorsConfig.class, JwtRequestFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, RateLimitConfiguration.class,
        RateLimitSecurityTest.ProbeController.class})
class RateLimitSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtUtil jwt;
    @MockitoBean MyUserDetailsService users;
    private static final AtomicInteger NEXT_IP = new AtomicInteger();
    private String ip;

    @BeforeEach
    void distinctSource() {
        ip = "203.0.113." + NEXT_IP.incrementAndGet();
    }

    @Test
    void sourceLimitStopsBeforeJwtAndCannotBeBypassedWithSpoofedHeaders() throws Exception {
        when(jwt.checkToken("invalid")).thenReturn(JwtUtil.TokenStatus.INVALID);
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr(ip); return r; })
                .header("Authorization", "Bearer invalid")).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr(ip); return r; })
                .header("Authorization", "Bearer invalid")
                .header("X-Forwarded-For", "198.51.100.7").header("Forwarded", "for=198.51.100.8")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Authorization, Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        verify(jwt, times(1)).checkToken("invalid");
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("198.51.100.1"); return r; }))
                .andExpect(status().isNoContent());
    }

    @Test
    void registrationVariantsShareBudgetButLoginIsSeparate() throws Exception {
        mvc.perform(post("/api/auth/register/candidate").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/register/company").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isNoContent());
    }

    @Test
    void verifiedUserBudgetSurvivesIpChangesAndOtherUsersStayIndependent() throws Exception {
        authenticate("alice-token", "alice@example.com");
        authenticate("bob-token", "bob@example.com");
        mvc.perform(post("/api/candidates/me/resume").header("Authorization", "Bearer alice-token")
                .with(r -> { r.setRemoteAddr(ip); return r; })).andExpect(status().isNoContent());
        mvc.perform(post("/api/candidates/me/photo").header("Authorization", "Bearer alice-token")
                .with(r -> { r.setRemoteAddr("198.51.100.11"); return r; }))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/candidates/me/resume").header("Authorization", "Bearer bob-token")
                .with(r -> { r.setRemoteAddr(ip); return r; })).andExpect(status().isNoContent());
    }

    @Test
    void preflightDoesNotConsumeAndAnonymousAuthorizationStillApplies() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .with(r -> { r.setRemoteAddr(ip); return r; })).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/candidates/me").with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private void authenticate(String token, String email) {
        when(jwt.checkToken(token)).thenReturn(JwtUtil.TokenStatus.VALID);
        when(jwt.getEmailFromToken(token)).thenReturn(email);
        when(users.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash").roles("CANDIDATE").build());
    }

    @RestController
    static class ProbeController {
        @RequestMapping(path = {"/api/auth/login", "/api/auth/register/candidate", "/api/auth/register/company",
                "/api/candidates/me/resume", "/api/candidates/me/photo"}, method = RequestMethod.POST)
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void mutation() {}
        @GetMapping("/api/candidates/me")
        String profile() { return "profile"; }
    }
}
