package com.unikly.matchingservice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.MatchEntry;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobMatchedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.matchingservice.domain.MatchResult;
import com.unikly.matchingservice.infrastructure.MatchResultRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class MatchingService {

    private final AiMatchingClient aiMatchingClient;
    private final MatchResultRepository matchResultRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private final Counter matchGeneratedCounter;
    private final Timer matchingDurationTimer;
    private final AtomicInteger matchingQueueSize = new AtomicInteger(0);

    @Value("${matching.max-results:20}")
    private int maxResults;

    public MatchingService(AiMatchingClient aiMatchingClient,
                           MatchResultRepository matchResultRepository,
                           OutboxRepository outboxRepository,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.aiMatchingClient = aiMatchingClient;
        this.matchResultRepository = matchResultRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;

        this.matchGeneratedCounter = Counter.builder("unikly_match_generated_total")
                .description("Total number of matches generated")
                .tag("strategy", "RULE_BASED")
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

            List<MatchResult> results = matchingDurationTimer.record(() ->
                    aiMatchingClient.computeMatches(
                            event.jobId(),
                            event.title(),
                            event.description(),
                            event.skills(),
                            event.budget() != null ? event.budget().amount() : null
                    )
            );

            matchResultRepository.saveAll(results);
            matchGeneratedCounter.increment(results.size());
            log.info("Stored {} match results for jobId={}", results.size(), event.jobId());

            publishJobMatchedEvent(event, results);
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

    private void publishJobMatchedEvent(JobCreatedEvent trigger, List<MatchResult> results) {
        List<MatchEntry> entries = results.stream()
                .map(r -> new MatchEntry(r.getFreelancerId(), r.getScore(), r.getMatchedSkills()))
                .toList();

        var event = new JobMatchedEvent(
                UUID.randomUUID(),
                "JobMatched",
                Instant.now(),
                trigger.jobId(),
                entries,
                "RULE_BASED"
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent("JobMatched", payload));
            log.info("Published JobMatchedEvent to outbox: jobId={}, matchCount={}",
                    trigger.jobId(), entries.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JobMatchedEvent for jobId={}", trigger.jobId(), e);
            throw new RuntimeException("Failed to publish JobMatchedEvent", e);
        }
    }
}
