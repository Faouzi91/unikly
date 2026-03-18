package com.unikly.searchservice.api.dto;

import java.time.Instant;
import java.util.List;

public record JobSearchResult(
        String jobId,
        String title,
        String description,
        List<String> skills,
        double budget,
        String currency,
        String status,
        String clientId,
        Instant createdAt,
        float score
) {
}
