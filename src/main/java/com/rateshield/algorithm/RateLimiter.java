package com.rateshield.algorithm;

public interface RateLimiter {
    boolean allowRequest(String clientId);
    int getRemainingRequests(String clientId);
    void reset(String clientId);
}
