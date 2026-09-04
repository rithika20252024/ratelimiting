package com.rateshield.repository;

import com.rateshield.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
    List<RateLimitConfig> findByEnabledTrue();
    Optional<RateLimitConfig> findByPathPattern(String pathPattern);
}
