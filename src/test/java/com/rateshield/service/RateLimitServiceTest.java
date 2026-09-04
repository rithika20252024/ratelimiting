package com.rateshield.service;

import com.rateshield.algorithm.RateLimiter;
import com.rateshield.algorithm.RateLimiterFactory;
import com.rateshield.model.RateLimitConfig;
import com.rateshield.model.RateLimitResponse;
import com.rateshield.repository.RateLimitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    @Mock
    private RateLimitConfigRepository configRepository;

    @Mock
    private RateLimiterFactory rateLimiterFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // Use constructor injection instead of @InjectMocks to avoid issues
        // with the ConcurrentHashMap field
        rateLimitService = new RateLimitService(configRepository, rateLimiterFactory, eventPublisher);
    }

    @Test
    @DisplayName("Should allow request when no matching config is found")
    void shouldAllowWhenNoConfigFound() {
        when(configRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        RateLimitResponse response = rateLimitService.checkRateLimit("client1", "/api/test");

        assertTrue(response.isAllowed());
        assertEquals("NONE", response.getAlgorithm());
    }

    @Test
    @DisplayName("Should allow request when rate limit is not exceeded")
    void shouldAllowRequestWithinLimit() {
        RateLimitConfig config = RateLimitConfig.builder()
                .pathPattern("/api/**")
                .algorithm("TOKEN_BUCKET")
                .maxRequests(10)
                .windowSeconds(60)
                .enabled(true)
                .build();
        when(configRepository.findByEnabledTrue()).thenReturn(List.of(config));

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(rateLimiterFactory.createRateLimiter("TOKEN_BUCKET", 10, 60)).thenReturn(mockLimiter);
        when(mockLimiter.allowRequest("client1")).thenReturn(true);
        when(mockLimiter.getRemainingRequests("client1")).thenReturn(9);

        RateLimitResponse response = rateLimitService.checkRateLimit("client1", "/api/test");

        assertTrue(response.isAllowed());
        assertEquals("TOKEN_BUCKET", response.getAlgorithm());
    }

    @Test
    @DisplayName("Should reject request and publish event when rate limit is exceeded")
    void shouldRejectAndPublishEventWhenLimitExceeded() {
        RateLimitConfig config = RateLimitConfig.builder()
                .pathPattern("/api/**")
                .algorithm("TOKEN_BUCKET")
                .maxRequests(10)
                .windowSeconds(60)
                .enabled(true)
                .build();
        when(configRepository.findByEnabledTrue()).thenReturn(List.of(config));

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(rateLimiterFactory.createRateLimiter("TOKEN_BUCKET", 10, 60)).thenReturn(mockLimiter);
        when(mockLimiter.allowRequest("client1")).thenReturn(false);

        RateLimitResponse response = rateLimitService.checkRateLimit("client1", "/api/test");

        assertFalse(response.isAllowed());
        assertEquals(0, response.getRemainingRequests());
        // Verify that an event was published (Observer pattern)
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("Should return correct remaining requests count in response")
    void shouldReturnRemainingRequestsInResponse() {
        RateLimitConfig config = RateLimitConfig.builder()
                .pathPattern("/api/**")
                .algorithm("SLIDING_WINDOW")
                .maxRequests(100)
                .windowSeconds(60)
                .enabled(true)
                .build();
        when(configRepository.findByEnabledTrue()).thenReturn(List.of(config));

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(rateLimiterFactory.createRateLimiter("SLIDING_WINDOW", 100, 60)).thenReturn(mockLimiter);
        when(mockLimiter.allowRequest("client1")).thenReturn(true);
        when(mockLimiter.getRemainingRequests("client1")).thenReturn(42);

        RateLimitResponse response = rateLimitService.checkRateLimit("client1", "/api/test");

        assertTrue(response.isAllowed());
        assertEquals(42, response.getRemainingRequests());
    }
}
