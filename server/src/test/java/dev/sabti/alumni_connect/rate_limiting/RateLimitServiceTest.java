package dev.sabti.alumni_connect.rate_limiting;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RateLimitServiceTest {
    private final TestClock clock = new TestClock();

    static RateLimitProperties properties() {
        var properties = new RateLimitProperties();
        for (String id : RateLimitProperties.POLICY_IDS) {
            var limit = new RateLimitProperties.Limit();
            limit.setCapacity(5);
            limit.setRefillPeriod(Duration.ofMinutes(1));
            properties.getPolicies().put(id, limit);
        }
        return properties;
    }

    @Test
    void firstRequestConsumesAllowanceAndKeysStayIndependent() {
        var service = new RateLimitService(properties(), clock);
        var policy = new RateLimitPolicy("upload", 1, 1, Duration.ofMinutes(1));
        assertThat(service.check(policy, "alice").allowed()).isTrue();
        assertThat(service.check(policy, "alice").allowed()).isFalse();
        assertThat(service.check(policy, "bob").allowed()).isTrue();
        assertThat(service.check(new RateLimitPolicy("search", 1, 1, Duration.ofMinutes(1)), "alice").allowed()).isTrue();
    }

    @Test
    void greedyRefillPreservesPartialTimeAndReturnsCeilingSeconds() {
        var service = new RateLimitService(properties(), clock);
        var policy = new RateLimitPolicy("upload", 1, 5, Duration.ofMinutes(1));
        service.check(policy, "alice");
        clock.advance(Duration.ofMillis(5500));
        assertThat(service.check(policy, "alice").retryAfterSeconds()).isEqualTo(7);
        clock.advance(Duration.ofMillis(6500));
        assertThat(service.check(policy, "alice").allowed()).isTrue();
        assertThat(service.check(policy, "alice").allowed()).isFalse();
    }

    @Test
    void activeBucketDoesNotResetAtItsCreationTimeExpiry() {
        var properties = properties();
        properties.setBucketIdleLifetime(Duration.ofMinutes(1));
        var service = new RateLimitService(properties, clock);
        var policy = new RateLimitPolicy("upload", 2, 2, Duration.ofMinutes(1));
        service.check(policy, "alice");
        service.check(policy, "alice");
        clock.advance(Duration.ofSeconds(45));
        assertThat(service.check(policy, "alice").allowed()).isTrue();
        clock.advance(Duration.ofSeconds(16));
        assertThat(service.check(policy, "alice").allowed()).isTrue();
        assertThat(service.check(policy, "alice").allowed()).isFalse();
    }

    @Test
    void simultaneousFirstRequestsCannotCreateExtraAllowance() throws Exception {
        var service = new RateLimitService(properties(), clock);
        var policy = new RateLimitPolicy("upload", 7, 7, Duration.ofHours(1));
        try (var pool = Executors.newFixedThreadPool(16)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 100; i++) tasks.add(() -> service.check(policy, "alice").allowed());
            int admitted = 0;
            for (var result : pool.invokeAll(tasks)) if (result.get()) admitted++;
            assertThat(admitted).isEqualTo(7);
        }
    }

    @Test
    void disabledLimiterDoesNotConsume() {
        var properties = properties();
        properties.setEnabled(false);
        var service = new RateLimitService(properties, clock);
        var policy = new RateLimitPolicy("upload", 1, 1, Duration.ofMinutes(1));
        for (int i = 0; i < 10; i++) assertThat(service.check(policy, "alice").allowed()).isTrue();
    }

    @Test
    void unsafeCacheLifetimeAndZeroRefillFailAtStartup() {
        var properties = properties();
        properties.setBucketIdleLifetime(Duration.ofSeconds(1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RateLimitService(properties, clock));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new RateLimitPolicy("bad", 1, 1, Duration.ZERO));
    }

    private static class TestClock implements TimeMeter {
        private final AtomicLong now = new AtomicLong();
        void advance(Duration duration) { now.addAndGet(duration.toNanos()); }
        public long currentTimeNanos() { return now.get(); }
        public boolean isWallClockBased() { return false; }
    }
}
