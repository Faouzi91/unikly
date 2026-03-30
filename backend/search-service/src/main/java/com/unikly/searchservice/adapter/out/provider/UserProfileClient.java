package com.unikly.searchservice.adapter.out.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    public UserProfileResponse fetchProfile(String userId) {
        try {
            return restClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/api/users/{id}", userId)
                    .retrieve()
                    .body(UserProfileResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch user profile: userId={}", userId, e);
            return null;
        }
    }

    public record UserProfileResponse(
            String id,
            String displayName,
            String bio,
            String avatarUrl,
            String role,
            List<String> skills,
            BigDecimal hourlyRate,
            String currency,
            String location,
            List<String> portfolioLinks,
            Double averageRating,
            Integer totalReviews
    ) {
    }
}
