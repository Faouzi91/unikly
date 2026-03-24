package com.unikly.jobservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to create a new job posting")
public record CreateJobRequest(

        @Schema(description = "Job title", example = "Build Angular Dashboard", maxLength = 200)
        @NotBlank @Size(max = 200)
        String title,

        @Schema(description = "Detailed job description", example = "We need an Angular developer to build a responsive admin dashboard.")
        @NotBlank
        String description,

        @Schema(description = "Fixed budget amount", example = "1500.00")
        @NotNull @Positive
        BigDecimal budget,

        @Schema(description = "ISO 4217 currency code — defaults to USD if omitted", example = "USD")
        String currency,

        @Schema(description = "Required skills for the job", example = "[\"Angular\", \"TypeScript\", \"Tailwind CSS\"]")
        @Size(min = 1)
        List<String> skills
) {}
