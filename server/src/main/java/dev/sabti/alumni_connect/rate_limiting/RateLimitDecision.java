package dev.sabti.alumni_connect.rate_limiting;

record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    static RateLimitDecision allow() {
        return new RateLimitDecision(true, 0);
    }

    static RateLimitDecision reject(long waitSeconds) {
        return new RateLimitDecision(false, Math.max(1, waitSeconds));
    }
}
