package com.unikly.matchingservice.application.service;

import com.unikly.matchingservice.domain.model.FreelancerSkillCache;
import com.unikly.matchingservice.domain.model.MatchResult;
import com.unikly.matchingservice.domain.model.MatchStrategy;
import com.unikly.matchingservice.application.port.out.FreelancerSkillCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedMatchingEngine implements MatchingEngine {

    private static final BigDecimal WEIGHT_SKILL    = new BigDecimal("0.40");
    private static final BigDecimal WEIGHT_BUDGET   = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_RATING   = new BigDecimal("0.25");
    private static final BigDecimal WEIGHT_RESPONSE = new BigDecimal("0.15");
    private static final BigDecimal RESPONSE_SCORE  = new BigDecimal("0.5");
    private static final int ESTIMATED_HOURS        = 160;

    private final FreelancerSkillCacheRepository freelancerSkillCacheRepository;

    @Value("${matching.max-results:20}")
    private int maxResults;

    @Override
    public List<MatchResult> computeMatches(UUID jobId,
                                            String title,
                                            String description,
                                            List<String> requiredSkills,
                                            BigDecimal budget) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            log.warn("No required skills for jobId={}, returning empty matches", jobId);
            return Collections.emptyList();
        }

        List<String> normalizedRequired = requiredSkills.stream()
                .map(String::toLowerCase)
                .toList();

        List<FreelancerSkillCache> candidates = freelancerSkillCacheRepository.findAll().stream()
                .filter(f -> f.getSkills() != null && !f.getSkills().isEmpty())
                .filter(f -> f.getSkills().stream()
                        .map(String::toLowerCase)
                        .anyMatch(normalizedRequired::contains))
                .toList();

        log.debug("Found {} candidate freelancers for jobId={}", candidates.size(), jobId);

        List<MatchResult> results = new ArrayList<>();
        for (FreelancerSkillCache freelancer : candidates) {
            List<String> matchedSkills = computeMatchedSkills(freelancer.getSkills(), normalizedRequired);
            BigDecimal score = computeScore(freelancer, matchedSkills, normalizedRequired, budget);

            results.add(MatchResult.builder()
                    .jobId(jobId)
                    .freelancerId(freelancer.getUserId())
                    .score(score)
                    .matchedSkills(matchedSkills)
                    .strategy(MatchStrategy.RULE_BASED)
                    .createdAt(Instant.now())
                    .build());
        }

        return results.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(maxResults)
                .toList();
    }

    private List<String> computeMatchedSkills(List<String> freelancerSkills, List<String> normalizedRequired) {
        return freelancerSkills.stream()
                .filter(s -> normalizedRequired.contains(s.toLowerCase()))
                .toList();
    }

    private BigDecimal computeScore(FreelancerSkillCache freelancer,
                                    List<String> matchedSkills,
                                    List<String> normalizedRequired,
                                    BigDecimal budget) {
        // Skill overlap: matched / required × 0.40
        BigDecimal skillOverlap = BigDecimal.valueOf((double) matchedSkills.size() / normalizedRequired.size())
                .multiply(WEIGHT_SKILL);

        // Budget compatibility: max(0, 1 - |hourlyRate × 160 - budget| / budget) × 0.20
        BigDecimal budgetScore = BigDecimal.ZERO;
        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0 && freelancer.getHourlyRate() != null) {
            BigDecimal estimatedCost = freelancer.getHourlyRate().multiply(BigDecimal.valueOf(ESTIMATED_HOURS));
            BigDecimal diff = estimatedCost.subtract(budget).abs();
            BigDecimal rawBudget = BigDecimal.ONE.subtract(diff.divide(budget, 4, RoundingMode.HALF_UP));
            budgetScore = rawBudget.max(BigDecimal.ZERO).multiply(WEIGHT_BUDGET);
        }

        // Rating score: (averageRating / 5.0) × 0.25
        BigDecimal ratingScore = BigDecimal.ZERO;
        if (freelancer.getAverageRating() != null) {
            ratingScore = freelancer.getAverageRating()
                    .divide(BigDecimal.valueOf(5.0), 4, RoundingMode.HALF_UP)
                    .multiply(WEIGHT_RATING);
        }

        // Response score: 0.5 × 0.15 (placeholder)
        BigDecimal responseScore = RESPONSE_SCORE.multiply(WEIGHT_RESPONSE);

        BigDecimal finalScore = skillOverlap.add(budgetScore).add(ratingScore).add(responseScore);

        // Clamp to [0.0, 1.0]
        finalScore = finalScore.min(BigDecimal.ONE).max(BigDecimal.ZERO);

        return finalScore.setScale(4, RoundingMode.HALF_UP);
    }
}
