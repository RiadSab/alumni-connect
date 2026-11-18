package dev.sabti.alumni_connect.rate_limiting;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthenticatedRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitPolicyResolver policies;
    private final RateLimitService limiter;
    private final RateLimitResponseWriter responses;

    public AuthenticatedRateLimitFilter(RateLimitPolicyResolver policies, RateLimitService limiter,
                                        RateLimitResponseWriter responses) {
        this.policies = policies;
        this.limiter = limiter;
        this.responses = responses;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            var policy = policies.authenticatedPolicy(request);
            if (policy.isPresent()) {
                var decision = limiter.check(policy.get(), "user:" + authentication.getName());
                if (!decision.allowed()) {
                    responses.write429(response, decision);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
