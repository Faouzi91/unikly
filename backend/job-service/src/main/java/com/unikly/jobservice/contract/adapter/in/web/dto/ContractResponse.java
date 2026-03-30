package com.unikly.jobservice.contract.adapter.in.web.dto;

import com.unikly.jobservice.contract.domain.model.ContractStatus;

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
