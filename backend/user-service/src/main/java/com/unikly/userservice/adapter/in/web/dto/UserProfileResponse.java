package com.unikly.userservice.adapter.in.web.dto;

import com.unikly.userservice.domain.model.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String displayName,
        String bio,
        String avatarUrl,
        UserRole role,
        List<String> skills,
        BigDecimal hourlyRate,
        String currency,
        String location,
        List<String> portfolioLinks,
        BigDecimal averageRating,
        int totalReviews,
        Instant createdAt,
        Instant updatedAt
) {}
