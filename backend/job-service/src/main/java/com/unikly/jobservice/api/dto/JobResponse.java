package com.unikly.jobservice.api.dto;

import com.unikly.jobservice.domain.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID clientId,
        String title,
        String description,
        BigDecimal budget,
        String currency,
        List<String> skills,
        JobStatus status,
        long proposalCount,
        Instant createdAt,
        Instant updatedAt
) {}
