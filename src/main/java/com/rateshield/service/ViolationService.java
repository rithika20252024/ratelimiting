package com.rateshield.service;

import com.rateshield.model.RateLimitViolation;
import com.rateshield.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViolationService {

    private final ViolationRepository violationRepository;

    public void recordViolation(String clientIp, String requestPath, String algorithm) {
        RateLimitViolation violation = new RateLimitViolation();
        violation.setClientIp(clientIp);
        violation.setRequestPath(requestPath);
        violation.setAlgorithm(algorithm);
        violation.setViolatedAt(LocalDateTime.now());
        violationRepository.save(violation);
    }

    public List<Object[]> getTopViolators() {
        return violationRepository.findTopViolators();
    }

    public long getViolationCountSince(LocalDateTime since) {
        return violationRepository.countByViolatedAtAfter(since);
    }

    public List<RateLimitViolation> getViolationsByClient(String clientIp) {
        return violationRepository.findByClientIp(clientIp);
    }
}
