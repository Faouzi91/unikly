package com.unikly.matchingservice.adapter.out.messaging;
import com.unikly.matchingservice.application.port.out.ProcessedEventRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.matchingservice.application.service.MatchingService;
import com.unikly.matchingservice.domain.model.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${matching.batch-delay-seconds:30}")
    private int batchDelaySeconds;

    @KafkaListener(topics = "job.events", groupId = "matching-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);

            switch (event) {
                case JobCreatedEvent e -> scheduleMatching(e);
                default -> log.debug("Ignoring unhandled event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process job event: {}", message, e);
            throw new RuntimeException("Failed to process job event", e);
        }
    }

    @Transactional
    public void handleJobCreated(JobCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate JobCreatedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        matchingService.processJobForMatching(event);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));

        log.info("Processed JobCreatedEvent: jobId={}, skills={}", event.jobId(), event.skills());
    }

    private void scheduleMatching(JobCreatedEvent event) {
        log.info("Scheduling matching for jobId={} with {}s delay", event.jobId(), batchDelaySeconds);
        scheduler.schedule(() -> {
            try {
                handleJobCreated(event);
            } catch (Exception e) {
                log.error("Failed to process scheduled matching for jobId={}", event.jobId(), e);
            }
        }, batchDelaySeconds, TimeUnit.SECONDS);
    }
}
