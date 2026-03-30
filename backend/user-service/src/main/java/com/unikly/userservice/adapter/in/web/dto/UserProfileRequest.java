package com.unikly.userservice.adapter.in.web.dto;

import com.unikly.userservice.domain.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to create or update a user profile")
public record UserProfileRequest(
        @Schema(description = "Display name shown to other users", example = "Jane Doe", maxLength = 100)
        @NotBlank @Size(max = 100) String displayName,

        @Schema(description = "User bio", example = "Full-stack developer with 8 years experience", maxLength = 5000)
        @Size(max = 5000) String bio,

        @Schema(description = "Avatar image URL") String avatarUrl,

        @Schema(description = "User role: CLIENT or FREELANCER") UserRole role,

        @Schema(description = "List of skills", example = "[\"Java\", \"Spring Boot\", \"Angular\"]") List<String> skills,

        @Schema(description = "Hourly rate in the specified currency", example = "75.00") BigDecimal hourlyRate,

        @Schema(description = "ISO 4217 currency code for hourly rate", example = "USD") String currency,

        @Schema(description = "User location", example = "Paris, France") String location,

        @Schema(description = "Portfolio URLs") List<String> portfolioLinks
) {}
