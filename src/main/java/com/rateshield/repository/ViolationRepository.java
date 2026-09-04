package com.rateshield.repository;

import com.rateshield.model.RateLimitViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<RateLimitViolation, Long> {
    List<RateLimitViolation> findByClientIp(String clientIp);

    @Query("SELECT v.clientIp, COUNT(v) as cnt FROM RateLimitViolation v GROUP BY v.clientIp ORDER BY cnt DESC")
    List<Object[]> findTopViolators();

    long countByViolatedAtAfter(LocalDateTime since);
}
