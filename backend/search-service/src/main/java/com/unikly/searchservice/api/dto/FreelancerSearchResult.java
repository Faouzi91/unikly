package com.unikly.searchservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Freelancer search result from Elasticsearch")
public record FreelancerSearchResult(
        @Schema(description = "User unique identifier") String userId,
        @Schema(description = "Freelancer display name") String displayName,
        @Schema(description = "Freelancer skills") List<String> skills,
        @Schema(description = "Hourly rate") double hourlyRate,
        @Schema(description = "Average rating (1.0–5.0)") double averageRating,
        @Schema(description = "Freelancer location") String location,
        @Schema(description = "Short bio") String bio,
        @Schema(description = "Relevance score from Elasticsearch") float score
) {
}
