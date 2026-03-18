package com.unikly.searchservice.api.dto;

import java.util.List;

public record FreelancerSearchResult(
        String userId,
        String displayName,
        List<String> skills,
        double hourlyRate,
        double averageRating,
        String location,
        String bio,
        float score
) {
}
