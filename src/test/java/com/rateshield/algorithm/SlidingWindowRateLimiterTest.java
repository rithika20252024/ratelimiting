package com.rateshield.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    @Test
    @DisplayName("shouldAllowRequestsWithinLimit")
    void shouldAllowRequestsWithinLimit() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(5, 60);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
    }

    @Test
    @DisplayName("shouldRejectWhenWindowFull")
    void shouldRejectWhenWindowFull() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 60);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
        assertFalse(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldAllowAfterWindowExpires")
    void shouldAllowAfterWindowExpires() throws InterruptedException {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        Thread.sleep(1100);
        assertTrue(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldTrackClientsIndependently")
    void shouldTrackClientsIndependently() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client2"));
    }

    @Test
    @DisplayName("shouldResetClientWindow")
    void shouldResetClientWindow() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        limiter.reset("client1");
        assertTrue(limiter.allowRequest("client1"));
    }
}
