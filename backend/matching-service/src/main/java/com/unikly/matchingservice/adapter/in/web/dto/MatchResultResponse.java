package com.unikly.matchingservice.adapter.in.web.dto;

import com.unikly.matchingservice.domain.model.MatchResult;
import com.unikly.matchingservice.domain.model.MatchStrategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResultResponse(
        UUID id,
        UUID jobId,
        UUID freelancerId,
        BigDecimal score,
        List<String> matchedSkills,
        MatchStrategy strategy,
        Instant createdAt
) {
    public static MatchResultResponse from(MatchResult entity) {
        return new MatchResultResponse(
                entity.getId(),
                entity.getJobId(),
                entity.getFreelancerId(),
                entity.getScore(),
                entity.getMatchedSkills(),
                entity.getStrategy(),
                entity.getCreatedAt()
        );
    }
}
