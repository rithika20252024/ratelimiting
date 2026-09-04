package com.rateshield.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Token Bucket Rate Limiting Algorithm.
 * 
 * The Token Bucket algorithm works by keeping a bucket that holds a maximum number of tokens.
 * Tokens are added to the bucket at a fixed rate.
 * When a request arrives, it must consume a token from the bucket to be processed.
 * If there are no tokens in the bucket, the request is dropped.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final int windowSeconds;
    private final double refillRate; // tokens per millisecond

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        // Calculate refill rate in tokens per millisecond
        this.refillRate = (double) maxRequests / (windowSeconds * 1000.0);
    }

    private class Bucket {
        double tokens;
        long lastRefillTime;

        Bucket(double tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    @Override
    public boolean allowRequest(String clientId) {
        Bucket bucket = buckets.computeIfAbsent(clientId, 
            k -> new Bucket(maxRequests, System.currentTimeMillis()));

        synchronized (bucket) {
            refill(bucket);
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    @Override
    public int getRemainingRequests(String clientId) {
        Bucket bucket = buckets.get(clientId);
        if (bucket == null) {
            return maxRequests;
        }
        synchronized (bucket) {
            refill(bucket);
            return (int) bucket.tokens;
        }
    }

    @Override
    public void reset(String clientId) {
        buckets.remove(clientId);
    }

    private void refill(Bucket bucket) {
        long now = System.currentTimeMillis();
        long elapsedTime = now - bucket.lastRefillTime;
        if (elapsedTime > 0) {
            double tokensToAdd = elapsedTime * refillRate;
            bucket.tokens = Math.min(maxRequests, bucket.tokens + tokensToAdd);
            bucket.lastRefillTime = now;
        }
    }
}
