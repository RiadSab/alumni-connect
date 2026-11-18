package dev.sabti.alumni_connect.rate_limiting;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RateLimitService {
    private final Cache<RateLimitKey, Bucket> buckets;
    private final TimeMeter clock;
    private final boolean enabled;

    public RateLimitService(RateLimitProperties properties) {
        this(properties, TimeMeter.SYSTEM_NANOTIME);
    }

    // One clock drives both refill and cache expiry, allowing deterministic tests.
    RateLimitService(RateLimitProperties properties, TimeMeter clock) {
        this.clock = clock;
        this.enabled = properties.isEnabled();
        properties.validate();
        buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.getBucketIdleLifetime())
                .maximumSize(properties.getMaximumBucketEntries())
                .ticker(clock::currentTimeNanos)
                .build();
    }

    RateLimitDecision check(RateLimitPolicy policy, String subject) {
        if (!enabled) return RateLimitDecision.allow();
        Objects.requireNonNull(subject, "Rate-limit subject is required");
        var key = new RateLimitKey(policy.id(), subject);
        // Atomic lookup/creation; the first request must consume a unit too.
        Bucket bucket = buckets.get(key, ignored -> createBucket(policy));
        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) return RateLimitDecision.allow();
        return RateLimitDecision.reject(Math.ceilDiv(
                probe.getNanosToWaitForRefill(), TimeUnit.SECONDS.toNanos(1)));
    }

    private Bucket createBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .withCustomTimePrecision(clock)
                .addLimit(limit -> limit.capacity(policy.capacity())
                        .refillGreedy(policy.refillTokens(), policy.refillPeriod()))
                .build();
    }
}
