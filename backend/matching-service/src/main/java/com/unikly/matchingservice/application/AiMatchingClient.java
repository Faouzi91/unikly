package com.unikly.matchingservice.application;

import com.unikly.matchingservice.domain.MatchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Client for the Python AI matching service (matching-ai:8090).
 * Currently falls back to {@link RuleBasedMatchingEngine} since the AI service
 * will be implemented in Step 6.1. The circuit breaker is configured and ready:
 * when the AI service is available, remove the UnsupportedOperationException and
 * implement the REST call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMatchingClient {

    private final RuleBasedMatchingEngine ruleBasedEngine;

    @Value("${matching.ai-service-url:http://matching-ai:8090}")
    private String aiServiceUrl;

    @CircuitBreaker(name = "ai-matching", fallbackMethod = "fallback")
    public List<MatchResult> computeMatches(UUID jobId, String title, String description,
                                             List<String> requiredSkills, BigDecimal budget) {
        // Step 6.1: replace this with actual REST call to AI service:
        // ResponseEntity<List<MatchResult>> response =
        //     restTemplate.exchange(aiServiceUrl + "/api/matches", POST, ...);
        // return response.getBody();

        // For now, throw so circuit breaker always falls back to rule-based engine:
        throw new UnsupportedOperationException("AI matching service not yet deployed");
    }

    @SuppressWarnings("unused")
    private List<MatchResult> fallback(UUID jobId, String title, String description,
                                        List<String> requiredSkills, BigDecimal budget,
                                        Throwable t) {
        log.warn("AI unavailable ({}), using rule-based matching for jobId={}", t.getMessage(), jobId);
        return ruleBasedEngine.computeMatches(jobId, title, description, requiredSkills, budget);
    }
}
