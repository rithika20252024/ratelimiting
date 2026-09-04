package com.rateshield.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("shouldAllowRequestsWithinLimit")
    void shouldAllowRequestsWithinLimit() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 60);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
    }

    @Test
    @DisplayName("shouldRejectRequestsOverLimit")
    void shouldRejectRequestsOverLimit() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 60);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
        assertFalse(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldTrackDifferentClientsSeparately")
    void shouldTrackDifferentClientsSeparately() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client2"));
    }

    @Test
    @DisplayName("shouldRefillTokensOverTime")
    void shouldRefillTokensOverTime() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 1);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        Thread.sleep(1100);
        assertTrue(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldResetClient")
    void shouldResetClient() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        limiter.reset("client1");
        assertTrue(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldReturnCorrectRemainingRequests")
    void shouldReturnCorrectRemainingRequests() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 60);
        assertEquals(5, limiter.getRemainingRequests("client1"));
        limiter.allowRequest("client1");
        limiter.allowRequest("client1");
        assertEquals(3, limiter.getRemainingRequests("client1"));
    }
}
