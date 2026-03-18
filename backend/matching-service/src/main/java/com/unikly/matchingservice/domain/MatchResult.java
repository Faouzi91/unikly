package com.unikly.matchingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "match_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "freelancer_id", nullable = false)
    private UUID freelancerId;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "matched_skills", nullable = false, columnDefinition = "text[]")
    private List<String> matchedSkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStrategy strategy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
