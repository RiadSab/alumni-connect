package dev.sabti.alumni_connect.rate_limiting;

import java.time.Duration;

record RateLimitPolicy(String id, long capacity, long refillTokens, Duration refillPeriod) {
    RateLimitPolicy {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Policy id must not be blank");
        if (capacity < 1) throw new IllegalArgumentException("Policy capacity must be >= 1");
        if (refillTokens < 1) throw new IllegalArgumentException("Policy refillTokens must be >= 1");
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("Policy refillPeriod must be positive");
        }
    }
}
