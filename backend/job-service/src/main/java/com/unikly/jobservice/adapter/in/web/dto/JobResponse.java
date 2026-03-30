package com.unikly.jobservice.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Job posting details")
public record JobResponse(
        @Schema(description = "Job unique identifier")
        UUID id,

        @Schema(description = "ID of the client who posted the job")
        UUID clientId,

        @Schema(description = "Job title")
        String title,

        @Schema(description = "Detailed job description")
        String description,

        @Schema(description = "Fixed budget amount", example = "1500.00")
        BigDecimal budget,

        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Required skills")
        List<String> skills,

        @Schema(description = "Current job status")
        String status,

        @Schema(description = "Optimistic lock version")
        int version,

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        Instant updatedAt
) {}
