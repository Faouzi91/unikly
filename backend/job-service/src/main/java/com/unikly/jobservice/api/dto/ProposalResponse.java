package com.unikly.jobservice.api.dto;

import com.unikly.jobservice.domain.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProposalResponse(
        UUID id,
        UUID jobId,
        UUID freelancerId,
        BigDecimal proposedBudget,
        String coverLetter,
        ProposalStatus status,
        Instant createdAt
) {}
