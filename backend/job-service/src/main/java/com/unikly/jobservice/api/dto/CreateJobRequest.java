package com.unikly.jobservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to create a new job posting")
public record CreateJobRequest(
        @Schema(description = "Job title", example = "Build Angular Dashboard", maxLength = 200)
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "Detailed job description", example = "We need an Angular 17 developer to build a responsive admin dashboard.")
        @NotBlank String description,

        @Schema(description = "Fixed budget amount", example = "1500.00")
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal budget,

        @Schema(description = "ISO 4217 currency code", example = "USD", maxLength = 3)
        @NotBlank @Size(max = 3) String currency,

        @Schema(description = "Required skills for the job", example = "[\"Angular\", \"TypeScript\", \"Tailwind CSS\"]")
        @NotEmpty List<String> skills
) {}
