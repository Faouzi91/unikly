package com.unikly.userservice.api.dto;

import com.unikly.userservice.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        String bio,
        String avatarUrl,
        UserRole role,
        List<String> skills,
        BigDecimal hourlyRate,
        String currency,
        String location,
        List<String> portfolioLinks
) {}
