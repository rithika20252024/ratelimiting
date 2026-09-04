package com.rateshield.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rate_limit_violation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientIp;
    private String requestPath;
    private String algorithm;

    @Column(name = "violated_at")
    private LocalDateTime violatedAt;

    @PrePersist
    public void prePersist() {
        if (this.violatedAt == null) {
            this.violatedAt = LocalDateTime.now();
        }
    }
}
