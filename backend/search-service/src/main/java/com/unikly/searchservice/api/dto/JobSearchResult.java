package com.unikly.searchservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Job search result from Elasticsearch")
public record JobSearchResult(
        @Schema(description = "Job unique identifier") String jobId,
        @Schema(description = "Job title") String title,
        @Schema(description = "Job description snippet") String description,
        @Schema(description = "Required skills") List<String> skills,
        @Schema(description = "Job budget") double budget,
        @Schema(description = "Currency code") String currency,
        @Schema(description = "Job status") String status,
        @Schema(description = "Client UUID") String clientId,
        @Schema(description = "Job creation timestamp") Instant createdAt,
        @Schema(description = "Relevance score from Elasticsearch") float score
) {
}
