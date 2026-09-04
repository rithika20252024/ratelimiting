package com.rateshield.integration;

import com.rateshield.algorithm.TokenBucketRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the RateShield rate limiting system.
 * 
 * These tests verify the end-to-end behavior of the rate limiting filter,
 * including HTTP response codes, headers, and concurrent thread safety.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Rate Limit Integration Tests")
class RateLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.rateshield.service.RateLimitService rateLimitService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rateLimitService.clearCache();
    }

    /**
     * Test configuration that provides a simple /api/test endpoint
     * for integration testing.
     */
    @TestConfiguration
    static class TestConfig {
        @RestController
        static class TestEndpointController {
            @GetMapping("/api/test")
            public String testEndpoint() {
                return "OK";
            }
        }
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit is exceeded")
    void shouldReturn429WhenRateLimitExceeded() {
        // data.sql seeds /api/** with max 10 requests per 60s window
        int maxRequests = 10;

        // Exhaust the rate limit
        for (int i = 0; i < maxRequests; i++) {
            ResponseEntity<String> resp = restTemplate.getForEntity("/api/test", String.class);
            assertEquals(HttpStatus.OK, resp.getStatusCode(),
                    "Request " + (i + 1) + " should be allowed");
        }

        // The next request should be rejected with 429
        ResponseEntity<String> response = restTemplate.getForEntity("/api/test", String.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode(),
                "Request " + (maxRequests + 1) + " should be rate limited");
    }

    /**
     * ⭐ THE STAR TEST — Proves thread safety of the rate limiter.
     * 
     * This test creates 20 concurrent threads that all try to make requests
     * simultaneously through a rate limiter with capacity 10.
     * 
     * Using CountDownLatch to synchronize thread start ensures maximum
     * contention on the shared data structure, proving thread safety.
     */
    @Test
    @DisplayName("Should handle concurrent requests safely without race conditions")
    void shouldHandleConcurrentRequestsSafely() throws InterruptedException {
        // Create a rate limiter with capacity 10
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 60);

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);       // Synchronize start
        CountDownLatch completionLatch = new CountDownLatch(threadCount); // Wait for all to finish

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        // Submit 20 concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // All threads wait here until released
                    if (limiter.allowRequest("testClient")) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Release all threads at once for maximum contention
        startLatch.countDown();
        completionLatch.await();

        // Verify: all 20 requests were processed (no lost updates)
        assertEquals(threadCount, allowed.get() + rejected.get(),
                "All requests should be accounted for");
        // Verify: exactly 10 were allowed (bucket capacity)
        assertEquals(10, allowed.get(),
                "Exactly 10 requests should be allowed (bucket capacity)");

        executorService.shutdown();
    }

    @Test
    @DisplayName("Should include rate limit headers in successful responses")
    void shouldReturnRateLimitHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/test", String.class);

        assertNotNull(response.getHeaders().get("X-RateLimit-Remaining"),
                "Response should contain X-RateLimit-Remaining header");
        assertNotNull(response.getHeaders().get("X-RateLimit-Algorithm"),
                "Response should contain X-RateLimit-Algorithm header");
    }
}
