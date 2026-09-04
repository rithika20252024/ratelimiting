package com.rateshield.service;

import com.rateshield.algorithm.RateLimiter;
import com.rateshield.algorithm.RateLimiterFactory;
import com.rateshield.model.RateLimitConfig;
import com.rateshield.model.RateLimitResponse;
import com.rateshield.observer.RateLimitEvent;
import com.rateshield.repository.RateLimitConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core service that coordinates rate limiting checks.
 * 
 * Uses a ConcurrentHashMap to cache RateLimiter instances per path pattern,
 * avoiding expensive re-creation on every request. The cache is invalidated
 * whenever rate limit configurations are updated via the ConfigController.
 */
@Slf4j
@Service
public class RateLimitService {

    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final RateLimiterFactory rateLimiterFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    // Cache of rate limiter instances, keyed by path pattern
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitConfigRepository rateLimitConfigRepository,
                            RateLimiterFactory rateLimiterFactory,
                            ApplicationEventPublisher applicationEventPublisher) {
        this.rateLimitConfigRepository = rateLimitConfigRepository;
        this.rateLimiterFactory = rateLimiterFactory;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Check if a request from a given client to a given path is within rate limits.
     *
     * @param clientId    the client identifier (e.g., IP address)
     * @param requestPath the API path being requested
     * @return RateLimitResponse indicating whether the request is allowed
     */
    public RateLimitResponse checkRateLimit(String clientId, String requestPath) {
        List<RateLimitConfig> configs = rateLimitConfigRepository.findByEnabledTrue();

        // Find the first matching config based on path pattern
        RateLimitConfig matchedConfig = configs.stream()
                .filter(c -> {
                    String pattern = c.getPathPattern();
                    if (pattern.endsWith("**")) {
                        String basePattern = pattern.substring(0, pattern.length() - 2);
                        return requestPath.startsWith(basePattern);
                    }
                    return requestPath.equals(pattern);
                })
                .findFirst()
                .orElse(null);

        if (matchedConfig == null) {
            return RateLimitResponse.builder()
                    .allowed(true)
                    .remainingRequests(-1)
                    .algorithm("NONE")
                    .message("Request allowed — no matching rate limit config")
                    .build();
        }

        // Get or create the rate limiter for this path pattern (thread-safe via computeIfAbsent)
        RateLimiter rateLimiter = rateLimiterCache.computeIfAbsent(
                matchedConfig.getPathPattern(),
                pattern -> rateLimiterFactory.createRateLimiter(
                        matchedConfig.getAlgorithm(),
                        matchedConfig.getMaxRequests(),
                        matchedConfig.getWindowSeconds()
                )
        );

        boolean allowed = rateLimiter.allowRequest(clientId);
        int remainingRequests = rateLimiter.getRemainingRequests(clientId);
        String algorithm = matchedConfig.getAlgorithm();

        if (!allowed) {
            log.warn("Rate limit exceeded for client: {} on path: {} using algorithm: {}", clientId, requestPath, algorithm);
            applicationEventPublisher.publishEvent(new RateLimitEvent(this, clientId, requestPath, algorithm));
            return RateLimitResponse.builder()
                    .allowed(false)
                    .remainingRequests(0)
                    .algorithm(algorithm)
                    .message("Rate limit exceeded")
                    .build();
        }

        return RateLimitResponse.builder()
                .allowed(true)
                .remainingRequests(remainingRequests)
                .algorithm(algorithm)
                .message("Request allowed")
                .build();
    }

    /**
     * Clear the rate limiter cache. Called when configurations are updated.
     */
    public void clearCache() {
        rateLimiterCache.clear();
        log.info("Rate limiter cache cleared.");
    }
}
