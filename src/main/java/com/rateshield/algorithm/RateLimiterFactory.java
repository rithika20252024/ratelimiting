package com.rateshield.algorithm;

import org.springframework.stereotype.Component;

@Component
public class RateLimiterFactory {

    public RateLimiter createRateLimiter(String algorithm, int maxRequests, int windowSeconds) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        switch (algorithm.toUpperCase()) {
            case "TOKEN_BUCKET":
                return new TokenBucketRateLimiter(maxRequests, windowSeconds);
            case "SLIDING_WINDOW":
                return new SlidingWindowRateLimiter(maxRequests, windowSeconds);
            case "FIXED_WINDOW":
                return new FixedWindowRateLimiter(maxRequests, windowSeconds);
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }
}
