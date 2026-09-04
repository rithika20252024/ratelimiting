package com.rateshield.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowRateLimiterTest {

    @Test
    @DisplayName("shouldAllowRequestsWithinWindow")
    void shouldAllowRequestsWithinWindow() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(5, 60);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
    }

    @Test
    @DisplayName("shouldRejectExcessRequests")
    void shouldRejectExcessRequests() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 60);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allowRequest("client1"));
        }
        assertFalse(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldResetCounterInNewWindow")
    void shouldResetCounterInNewWindow() throws InterruptedException {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 1);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        Thread.sleep(1100);
        assertTrue(limiter.allowRequest("client1"));
    }

    @Test
    @DisplayName("shouldHandleMultipleClients")
    void shouldHandleMultipleClients() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client1"));
        assertTrue(limiter.allowRequest("client2"));
    }

    @Test
    @DisplayName("shouldResetSpecificClient")
    void shouldResetSpecificClient() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 60);
        assertTrue(limiter.allowRequest("client1"));
        assertFalse(limiter.allowRequest("client1"));
        limiter.reset("client1");
        assertTrue(limiter.allowRequest("client1"));
    }
}
