package com.rateshield.algorithm;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log Rate Limiting Algorithm.
 * 
 * The algorithm keeps track of request timestamps for each client.
 * When a new request arrives, it removes all timestamps older than the window size.
 * If the number of remaining timestamps is less than the max allowed, the request is allowed.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Deque<Long>> clientWindows = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    public boolean allowRequest(String clientId) {
        Deque<Long> window = clientWindows.computeIfAbsent(clientId, k -> new LinkedList<>());
        long now = System.currentTimeMillis();

        synchronized (window) {
            cleanExpired(window, now);
            if (window.size() < maxRequests) {
                window.addLast(now);
                return true;
            }
            return false;
        }
    }

    @Override
    public int getRemainingRequests(String clientId) {
        Deque<Long> window = clientWindows.get(clientId);
        if (window == null) {
            return maxRequests;
        }

        synchronized (window) {
            cleanExpired(window, System.currentTimeMillis());
            return Math.max(0, maxRequests - window.size());
        }
    }

    @Override
    public void reset(String clientId) {
        clientWindows.remove(clientId);
    }

    private void cleanExpired(Deque<Long> window, long now) {
        while (!window.isEmpty() && now - window.peekFirst() >= windowMillis) {
            window.pollFirst();
        }
    }
}
