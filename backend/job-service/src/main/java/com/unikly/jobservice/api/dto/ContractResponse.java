package com.unikly.jobservice.api.dto;

import com.unikly.jobservice.domain.ContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID jobId,
        UUID clientId,
        UUID freelancerId,
        BigDecimal agreedBudget,
        String terms,
        ContractStatus status,
        Instant startedAt,
        Instant completedAt
) {}
