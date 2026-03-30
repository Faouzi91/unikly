package com.unikly.notificationservice.adapter.out.messaging;
import com.unikly.notificationservice.application.port.out.JobClientCacheRepository;
import com.unikly.notificationservice.application.port.out.ProcessedEventRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.JobCancelledEvent;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobUpdatedEvent;
import com.unikly.common.events.ProposalAcceptedEvent;
import com.unikly.common.events.ProposalSubmittedEvent;
import com.unikly.notificationservice.application.service.NotificationDeliveryService;
import com.unikly.notificationservice.domain.model.JobClientCache;
import com.unikly.notificationservice.domain.model.NotificationType;
import com.unikly.notificationservice.domain.model.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final NotificationDeliveryService deliveryService;
    private final JobClientCacheRepository jobClientCacheRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "job.events", groupId = "notification-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);
            switch (event) {
                case JobCreatedEvent e       -> handleJobCreated(e);
                case JobUpdatedEvent e       -> handleJobUpdated(e);
                case JobCancelledEvent e     -> handleJobCancelled(e);
                case ProposalSubmittedEvent e -> handleProposalSubmitted(e);
                case ProposalAcceptedEvent e  -> handleProposalAccepted(e);
                default -> log.debug("Ignoring event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process job event: {}", message, e);
            throw new RuntimeException("Failed to process job event", e);
        }
    }

    @Transactional
    public void handleJobCreated(JobCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        // Cache job → client mapping for later lookups
        var cache = JobClientCache.builder()
                .jobId(event.jobId())
                .clientId(event.clientId())
                .title(event.title())
                .build();
        jobClientCacheRepository.save(cache);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
        log.debug("Cached job client mapping: jobId={}, clientId={}", event.jobId(), event.clientId());
    }

    @Transactional
    public void handleJobUpdated(JobUpdatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        jobClientCacheRepository.findById(event.jobId()).ifPresent(job -> {
            String changedSummary = String.join(", ", event.changedFields());
            String body = "The job '" + job.getTitle() + "' has been updated"
                    + (changedSummary.isBlank() ? "." : " (changed: " + changedSummary + ").");

            for (var freelancerId : event.affectedFreelancerIds()) {
                deliveryService.createAndDeliver(
                        freelancerId,
                        NotificationType.SYSTEM,
                        "Job updated — your proposal may be outdated",
                        body,
                        "/jobs/" + event.jobId());
            }
        });

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @Transactional
    public void handleJobCancelled(JobCancelledEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        jobClientCacheRepository.findById(event.jobId()).ifPresent(job -> {
            String body = "The job '" + job.getTitle() + "' has been cancelled.";

            for (var freelancerId : event.affectedFreelancerIds()) {
                deliveryService.createAndDeliver(
                        freelancerId,
                        NotificationType.SYSTEM,
                        "Job cancelled",
                        body,
                        "/jobs/" + event.jobId());
            }

            deliveryService.createAndDeliver(
                    event.clientId(),
                    NotificationType.SYSTEM,
                    "Job cancelled",
                    body,
                    "/jobs/" + event.jobId());
        });

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @Transactional
    public void handleProposalSubmitted(ProposalSubmittedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        jobClientCacheRepository.findById(event.jobId()).ifPresentOrElse(
                job -> deliveryService.createAndDeliver(
                        job.getClientId(),
                        NotificationType.PROPOSAL_RECEIVED,
                        "New proposal received",
                        "A freelancer submitted a proposal on your job '" + job.getTitle() + "'",
                        "/jobs/" + event.jobId()),
                () -> log.warn("JobClientCache miss for ProposalSubmittedEvent: jobId={}", event.jobId()));

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @Transactional
    public void handleProposalAccepted(ProposalAcceptedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        // Update cache with freelancerId
        jobClientCacheRepository.findById(event.jobId()).ifPresent(job -> {
            job.setFreelancerId(event.freelancerId());
            jobClientCacheRepository.save(job);
        });

        // Notify the freelancer
        deliveryService.createAndDeliver(
                event.freelancerId(),
                NotificationType.PROPOSAL_ACCEPTED,
                "Your proposal was accepted!",
                "Congratulations! Your proposal for a job has been accepted. Work can now begin.",
                "/jobs/" + event.jobId());

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
