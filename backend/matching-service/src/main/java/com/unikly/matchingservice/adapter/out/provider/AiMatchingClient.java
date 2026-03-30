package com.unikly.matchingservice.adapter.out.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unikly.matchingservice.domain.model.FreelancerSkillCache;
import com.unikly.matchingservice.domain.model.MatchResult;
import com.unikly.matchingservice.domain.model.MatchStrategy;
import com.unikly.matchingservice.application.port.out.FreelancerSkillCacheRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client for the Python AI matching service (matching-ai:8090).
 * Sends job details and a list of pre-filtered freelancer profiles, receives
 * cosine-similarity scores, and converts them into {@link MatchResult} entities
 * with strategy {@code AI_EMBEDDING}.
 *
 * <p>The circuit breaker ({@code ai-matching}) opens on repeated failures and
 * returns an empty list, allowing {@link MatchingService} to fall back to
 * 100 % rule-based scoring.
 */
@Slf4j
@Component
public class AiMatchingClient {

    private final FreelancerSkillCacheRepository freelancerSkillCacheRepository;
    private final RestClient restClient;

    @Value("${matching.ai-service-url:http://matching-ai:8090}")
    private String aiServiceUrl;

    public AiMatchingClient(FreelancerSkillCacheRepository freelancerSkillCacheRepository,
                            RestClient.Builder restClientBuilder) {
        this.freelancerSkillCacheRepository = freelancerSkillCacheRepository;
        this.restClient = restClientBuilder.build();
    }

    @CircuitBreaker(name = "ai-matching", fallbackMethod = "fallback")
    public List<MatchResult> computeMatches(UUID jobId, String title, String description,
                                             List<String> requiredSkills, BigDecimal budget) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedRequired = requiredSkills.stream()
                .map(String::toLowerCase)
                .toList();

        // Pre-filter to freelancers that share at least one required skill
        List<FreelancerSkillCache> candidates = freelancerSkillCacheRepository.findAll().stream()
                .filter(f -> f.getSkills() != null && !f.getSkills().isEmpty())
                .filter(f -> f.getSkills().stream()
                        .map(String::toLowerCase)
                        .anyMatch(normalizedRequired::contains))
                .toList();

        if (candidates.isEmpty()) {
            log.debug("No skill-matched candidates for jobId={}", jobId);
            return Collections.emptyList();
        }

        List<AiFreelancerProfile> profiles = candidates.stream()
                .map(f -> new AiFreelancerProfile(
                        f.getUserId().toString(),
                        "",   // bio not cached locally; embedding still leverages skills
                        f.getSkills() != null ? f.getSkills() : List.of(),
                        f.getHourlyRate() != null ? f.getHourlyRate().doubleValue() : 0.0,
                        f.getAverageRating() != null ? f.getAverageRating().doubleValue() : 0.0))
                .toList();

        var request = new AiMatchRequest(
                jobId.toString(),
                title + " " + description,
                requiredSkills,
                profiles);

        log.info("Calling AI matching service for jobId={}, candidates={}", jobId, candidates.size());

        AiMatchResponse response = restClient.post()
                .uri(aiServiceUrl + "/api/ai/match")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiMatchResponse.class);

        if (response == null || response.matches() == null) {
            log.warn("AI service returned null response for jobId={}", jobId);
            return Collections.emptyList();
        }

        log.info("AI service returned {} matches for jobId={}", response.matches().size(), jobId);

        return response.matches().stream()
                .map(m -> MatchResult.builder()
                        .jobId(jobId)
                        .freelancerId(UUID.fromString(m.freelancerId()))
                        .score(BigDecimal.valueOf(m.score()).setScale(4, RoundingMode.HALF_UP))
                        .matchedSkills(m.matchedSkills() != null ? m.matchedSkills() : List.of())
                        .strategy(MatchStrategy.AI_EMBEDDING)
                        .createdAt(Instant.now())
                        .build())
                .toList();
    }

    @SuppressWarnings("unused")
    private List<MatchResult> fallback(UUID jobId, String title, String description,
                                        List<String> requiredSkills, BigDecimal budget,
                                        Throwable t) {
        log.warn("AI matching circuit open ({}) — returning empty for jobId={}", t.getMessage(), jobId);
        return Collections.emptyList();
    }

    // ── Internal DTOs (snake_case for Python API compatibility) ──────────────

    private record AiFreelancerProfile(
            @JsonProperty("user_id") String userId,
            String bio,
            List<String> skills,
            @JsonProperty("hourly_rate") double hourlyRate,
            @JsonProperty("average_rating") double averageRating) {}

    private record AiMatchRequest(
            @JsonProperty("job_id") String jobId,
            @JsonProperty("job_description") String jobDescription,
            @JsonProperty("job_skills") List<String> jobSkills,
            List<AiFreelancerProfile> freelancers) {}

    private record AiMatchScore(
            @JsonProperty("freelancer_id") String freelancerId,
            double score,
            @JsonProperty("matched_skills") List<String> matchedSkills) {}

    private record AiMatchResponse(
            @JsonProperty("job_id") String jobId,
            List<AiMatchScore> matches) {}
}
