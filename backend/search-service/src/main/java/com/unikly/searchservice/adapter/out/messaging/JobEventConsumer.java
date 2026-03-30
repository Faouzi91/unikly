package com.unikly.searchservice.adapter.out.messaging;
import com.unikly.searchservice.application.port.out.JobDocumentRepository;
import com.unikly.searchservice.application.port.out.ProcessedEventDocumentRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobStatusChangedEvent;
import com.unikly.searchservice.domain.model.JobDocument;
import com.unikly.searchservice.domain.model.ProcessedEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private static final Set<String> REMOVE_STATUSES = Set.of("CANCELLED", "CLOSED");

    private final JobDocumentRepository jobDocumentRepository;
    private final ProcessedEventDocumentRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "job.events", groupId = "search-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);

            switch (event) {
                case JobCreatedEvent e -> handleJobCreated(e);
                case JobStatusChangedEvent e -> handleJobStatusChanged(e);
                default -> log.debug("Ignoring unhandled event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process job event: {}", message, e);
            throw new RuntimeException("Failed to process job event", e);
        }
    }

    private void handleJobCreated(JobCreatedEvent event) {
        if (isAlreadyProcessed(event.eventId().toString())) {
            log.info("Duplicate JobCreatedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        var doc = JobDocument.builder()
                .jobId(event.jobId().toString())
                .title(event.title())
                .description(event.description())
                .skills(event.skills())
                .budget(event.budget().amount().doubleValue())
                .currency(event.budget().currency())
                .status(event.status())
                .clientId(event.clientId().toString())
                .createdAt(event.timestamp())
                .build();

        jobDocumentRepository.save(doc);
        markProcessed(event.eventId().toString());
        log.info("Indexed new job: jobId={}", event.jobId());
    }

    private void handleJobStatusChanged(JobStatusChangedEvent event) {
        if (isAlreadyProcessed(event.eventId().toString())) {
            log.info("Duplicate JobStatusChangedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        var jobId = event.jobId().toString();

        if (REMOVE_STATUSES.contains(event.newStatus())) {
            jobDocumentRepository.deleteById(jobId);
            log.info("Removed job from index: jobId={}, newStatus={}", jobId, event.newStatus());
        } else {
            jobDocumentRepository.findById(jobId).ifPresent(doc -> {
                doc.setStatus(event.newStatus());
                jobDocumentRepository.save(doc);
                log.info("Updated job status in index: jobId={}, {} → {}", jobId, event.previousStatus(), event.newStatus());
            });
        }

        markProcessed(event.eventId().toString());
    }

    private boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEventDocument(eventId, Instant.now()));
    }
}
