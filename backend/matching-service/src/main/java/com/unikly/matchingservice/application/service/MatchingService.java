package com.unikly.matchingservice.application.service;
import com.unikly.matchingservice.adapter.out.provider.AiMatchingClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.MatchEntry;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobMatchedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.matchingservice.domain.model.MatchResult;
import com.unikly.matchingservice.domain.model.MatchStrategy;
import com.unikly.matchingservice.application.port.out.MatchResultRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchingService {

    private final AiMatchingClient aiMatchingClient;
    private final RuleBasedMatchingEngine ruleBasedEngine;
    private final MatchResultRepository matchResultRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private final Counter matchGeneratedCounter;
    private final Timer matchingDurationTimer;
    private final AtomicInteger matchingQueueSize = new AtomicInteger(0);

    @Value("${matching.max-results:20}")
    private int maxResults;

    public MatchingService(AiMatchingClient aiMatchingClient,
                           RuleBasedMatchingEngine ruleBasedEngine,
                           MatchResultRepository matchResultRepository,
                           OutboxRepository outboxRepository,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.aiMatchingClient = aiMatchingClient;
        this.ruleBasedEngine = ruleBasedEngine;
        this.matchResultRepository = matchResultRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;

        this.matchGeneratedCounter = Counter.builder("unikly_match_generated_total")
                .description("Total number of matches generated")
                .tag("strategy", "HYBRID")
                .register(meterRegistry);
        this.matchingDurationTimer = Timer.builder("unikly_matching_duration_seconds")
                .description("Time spent computing matches")
                .register(meterRegistry);
        Gauge.builder("unikly_matching_queue_size", matchingQueueSize, AtomicInteger::get)
                .description("Current number of matching jobs in queue")
                .register(meterRegistry);
    }

    @Transactional
    public void processJobForMatching(JobCreatedEvent event) {
        log.info("Running matching for jobId={}, skills={}", event.jobId(), event.skills());
        matchingQueueSize.incrementAndGet();

        try {
            matchResultRepository.deleteByJobId(event.jobId());

            BigDecimal budget = event.budget() != null ? event.budget().amount() : null;

            List<MatchResult> results = matchingDurationTimer.record(() -> {
                // 1. Try AI matching (circuit breaker returns empty on failure)
                List<MatchResult> aiResults = aiMatchingClient.computeMatches(
                        event.jobId(), event.title(), event.description(), event.skills(), budget);

                // 2. Always run rule-based matching
                List<MatchResult> ruleResults = ruleBasedEngine.computeMatches(
                        event.jobId(), event.title(), event.description(), event.skills(), budget);

                // 3. If AI circuit is open / unavailable → 100 % rule-based
                if (aiResults.isEmpty()) {
                    log.warn("AI circuit open or no results for jobId={} — using 100% rule-based", event.jobId());
                    return ruleResults;
                }

                // 4. Hybrid: 50 % AI + 50 % rule-based
                log.info("Computing hybrid scores for jobId={} (ai={}, rule={})",
                        event.jobId(), aiResults.size(), ruleResults.size());
                return computeHybridScores(event.jobId(), aiResults, ruleResults);
            });

            matchResultRepository.saveAll(results);
            matchGeneratedCounter.increment(results.size());
            log.info("Stored {} match results for jobId={}", results.size(), event.jobId());

            boolean usedAi = results.stream().anyMatch(r -> r.getStrategy() == MatchStrategy.AI_EMBEDDING);
            publishJobMatchedEvent(event, results, usedAi ? "AI_EMBEDDING" : "RULE_BASED");

        } finally {
            matchingQueueSize.decrementAndGet();
        }
    }

    public List<MatchResult> getMatchesForJob(UUID jobId, int limit) {
        return matchResultRepository.findByJobIdOrderByScoreDesc(
                jobId, PageRequest.of(0, Math.min(limit, maxResults)));
    }

    public List<MatchResult> getMatchesForFreelancer(UUID freelancerId, int limit) {
        return matchResultRepository.findByFreelancerIdOrderByScoreDesc(
                freelancerId, PageRequest.of(0, Math.min(limit, maxResults)));
    }

    public MatchResult getMatch(UUID id) {
        return matchResultRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match result not found: " + id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<MatchResult> computeHybridScores(UUID jobId,
                                                   List<MatchResult> aiResults,
                                                   List<MatchResult> ruleResults) {
        Map<UUID, BigDecimal> aiScores = aiResults.stream()
                .collect(Collectors.toMap(MatchResult::getFreelancerId, MatchResult::getScore));
        Map<UUID, BigDecimal> ruleScores = ruleResults.stream()
                .collect(Collectors.toMap(MatchResult::getFreelancerId, MatchResult::getScore));
        Map<UUID, List<String>> aiSkills = aiResults.stream()
                .collect(Collectors.toMap(MatchResult::getFreelancerId, MatchResult::getMatchedSkills));
        Map<UUID, List<String>> ruleSkills = ruleResults.stream()
                .collect(Collectors.toMap(MatchResult::getFreelancerId, MatchResult::getMatchedSkills));

        Set<UUID> allFreelancers = new HashSet<>(aiScores.keySet());
        allFreelancers.addAll(ruleScores.keySet());

        return allFreelancers.stream()
                .map(freelancerId -> {
                    BigDecimal aiScore   = aiScores.getOrDefault(freelancerId, BigDecimal.ZERO);
                    BigDecimal ruleScore = ruleScores.getOrDefault(freelancerId, BigDecimal.ZERO);
                    BigDecimal hybrid    = aiScore.multiply(new BigDecimal("0.50"))
                            .add(ruleScore.multiply(new BigDecimal("0.50")))
                            .setScale(4, RoundingMode.HALF_UP);

                    List<String> matchedSkills = aiSkills.containsKey(freelancerId)
                            ? aiSkills.get(freelancerId)
                            : ruleSkills.getOrDefault(freelancerId, List.of());

                    return MatchResult.builder()
                            .jobId(jobId)
                            .freelancerId(freelancerId)
                            .score(hybrid)
                            .matchedSkills(matchedSkills)
                            .strategy(MatchStrategy.AI_EMBEDDING)
                            .createdAt(Instant.now())
                            .build();
                })
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(maxResults)
                .toList();
    }

    private void publishJobMatchedEvent(JobCreatedEvent trigger, List<MatchResult> results, String strategy) {
        List<MatchEntry> entries = results.stream()
                .map(r -> new MatchEntry(r.getFreelancerId(), r.getScore(), r.getMatchedSkills()))
                .toList();

        var event = new JobMatchedEvent(
                UUID.randomUUID(),
                "JobMatched",
                Instant.now(),
                trigger.jobId(),
                entries,
                strategy
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent("JobMatched", trigger.jobId(), "Job", payload));
            log.info("Published JobMatchedEvent to outbox: jobId={}, matchCount={}, strategy={}",
                    trigger.jobId(), entries.size(), strategy);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JobMatchedEvent for jobId={}", trigger.jobId(), e);
            throw new RuntimeException("Failed to publish JobMatchedEvent", e);
        }
    }
}
