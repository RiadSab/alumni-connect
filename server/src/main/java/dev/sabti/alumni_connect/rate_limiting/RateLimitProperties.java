package dev.sabti.alumni_connect.rate_limiting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties("app.rate-limit")
public class RateLimitProperties {
    static final List<String> POLICY_IDS = List.of(
            "source-default", "source-public-read", "source-login", "source-registration",
            "source-forgot-password", "source-reset-password", "source-refresh", "source-logout",
            "source-confirmation", "user-default", "user-upload", "user-change-password");
    private boolean enabled = true;
    private long maximumBucketEntries = 10_000;
    private Duration bucketIdleLifetime = Duration.ofHours(2);
    private Map<String, Limit> policies = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Limit {
        private long capacity;
        private Long refillTokens;
        private Duration refillPeriod;

        RateLimitPolicy toPolicy(String id) {
            return new RateLimitPolicy(id, capacity,
                    refillTokens == null ? capacity : refillTokens, refillPeriod);
        }
    }

    void validate() {
        if (maximumBucketEntries < 1 || bucketIdleLifetime == null
                || bucketIdleLifetime.isNegative() || bucketIdleLifetime.isZero()) {
            throw new IllegalArgumentException("Rate-limit cache size and idle lifetime must be positive");
        }
        if (!enabled) return;
        for (String id : POLICY_IDS) {
            Limit limit = policies.get(id);
            if (limit == null) throw new IllegalArgumentException("Missing rate-limit policy: " + id);
            RateLimitPolicy policy = limit.toPolicy(id);
            // Idle expiry must not grant an earlier reset than a full refill.
            BigInteger required = BigInteger.valueOf(policy.refillPeriod().toNanos())
                    .multiply(BigInteger.valueOf(policy.capacity()));
            BigInteger retained = BigInteger.valueOf(bucketIdleLifetime.toNanos())
                    .multiply(BigInteger.valueOf(policy.refillTokens()));
            if (retained.compareTo(required) < 0) {
                throw new IllegalArgumentException("Bucket idle lifetime is shorter than full refill for " + id);
            }
        }
    }
}
