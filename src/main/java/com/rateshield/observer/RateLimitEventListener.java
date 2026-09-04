package com.rateshield.observer;

import com.rateshield.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitEventListener {

    private final ViolationService violationService;

    @EventListener
    public void handleRateLimitEvent(RateLimitEvent event) {
        log.warn("Rate limit exceeded for client {} on path {}", event.getClientIp(), event.getRequestPath());
        violationService.recordViolation(event.getClientIp(), event.getRequestPath(), event.getAlgorithm());
    }
}
