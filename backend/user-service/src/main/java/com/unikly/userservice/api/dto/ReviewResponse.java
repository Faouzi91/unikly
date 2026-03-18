package com.unikly.userservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID reviewerId,
        UUID revieweeId,
        UUID jobId,
        int rating,
        String comment,
        Instant createdAt
) {}
