package com.rateshield.controller;

import com.rateshield.model.RateLimitViolation;
import com.rateshield.repository.RateLimitConfigRepository;
import com.rateshield.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/management/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ViolationService violationService;
    private final RateLimitConfigRepository rateLimitConfigRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConfigs", rateLimitConfigRepository.count());
        stats.put("totalViolationsLast24h", violationService.getViolationCountSince(LocalDateTime.now().minusHours(24)));
        stats.put("topViolators", violationService.getTopViolators());
        return stats;
    }

    @GetMapping("/violations/{clientIp}")
    public List<RateLimitViolation> getViolationsByClient(@PathVariable String clientIp) {
        return violationService.getViolationsByClient(clientIp);
    }
}
