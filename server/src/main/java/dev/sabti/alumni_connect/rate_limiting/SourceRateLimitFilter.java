package dev.sabti.alumni_connect.rate_limiting;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SourceRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitPolicyResolver policies;
    private final ClientIpResolver clientIps;
    private final RateLimitService limiter;
    private final RateLimitResponseWriter responses;

    public SourceRateLimitFilter(RateLimitPolicyResolver policies, ClientIpResolver clientIps,
                                 RateLimitService limiter, RateLimitResponseWriter responses) {
        this.policies = policies;
        this.clientIps = clientIps;
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
        var policy = policies.sourcePolicy(request);
        if (policy.isPresent()) {
            var decision = limiter.check(policy.get(), "ip:" + clientIps.resolve(request));
            if (!decision.allowed()) {
                responses.write429(response, decision);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
