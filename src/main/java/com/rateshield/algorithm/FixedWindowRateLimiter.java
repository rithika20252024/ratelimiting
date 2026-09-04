package com.rateshield.algorithm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed Window Counter Rate Limiting Algorithm.
 * 
 * The algorithm divides time into fixed windows and keeps a counter for each window.
 * When a request arrives, the counter for the current window is incremented.
 * If the counter exceeds the max allowed, the request is dropped.
 */
public class FixedWindowRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, WindowCounter> clientCounters = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    private class WindowCounter {
        AtomicInteger count;
        long windowStart;

        WindowCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }

    @Override
    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        WindowCounter counter = clientCounters.computeIfAbsent(clientId, 
            k -> new WindowCounter(now, 0));

        synchronized (counter) {
            if (now - counter.windowStart >= windowMillis) {
                // reset window
                counter.windowStart = now;
                counter.count.set(0);
            }

            if (counter.count.get() < maxRequests) {
                counter.count.incrementAndGet();
                return true;
            }
            return false;
        }
    }

    @Override
    public int getRemainingRequests(String clientId) {
        WindowCounter counter = clientCounters.get(clientId);
        if (counter == null) {
            return maxRequests;
        }

        long now = System.currentTimeMillis();
        synchronized (counter) {
            if (now - counter.windowStart >= windowMillis) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - counter.count.get());
        }
    }

    @Override
    public void reset(String clientId) {
        clientCounters.remove(clientId);
    }
}
