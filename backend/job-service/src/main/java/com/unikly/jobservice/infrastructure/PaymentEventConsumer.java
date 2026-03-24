package com.unikly.jobservice.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.EscrowReleasedEvent;
import com.unikly.common.events.JobStatusChangedEvent;
import com.unikly.common.events.PaymentCompletedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.jobservice.domain.ContractStatus;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.domain.JobStateMachine;
import com.unikly.jobservice.domain.ProcessedEvent;
import com.unikly.jobservice.domain.ProposalStatus;
import com.unikly.jobservice.infrastructure.repository.ContractRepository;
import com.unikly.jobservice.infrastructure.repository.JobRepository;
import com.unikly.jobservice.infrastructure.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final JobRepository jobRepository;
    private final ProposalRepository proposalRepository;
    private final ContractRepository contractRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.events", groupId = "job-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);

            switch (event) {
                case PaymentCompletedEvent e -> handlePaymentCompleted(e);
                case EscrowReleasedEvent e -> handleEscrowReleased(e);
                default -> log.debug("Ignoring unhandled event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (isAlreadyProcessed(event.eventId())) {
            log.info("Duplicate PaymentCompletedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        var jobOpt = jobRepository.findById(event.jobId());
        if (jobOpt.isEmpty()) {
            log.warn("Job not found for PaymentCompletedEvent: jobId={}", event.jobId());
            markProcessed(event.eventId());
            return;
        }

        var job = jobOpt.get();

        if (job.getStatus() != JobStatus.IN_REVIEW) {
            log.warn("Received PaymentCompletedEvent but job {} is in status {}, expected IN_REVIEW. Skipping.",
                    event.jobId(), job.getStatus());
            markProcessed(event.eventId());
            return;
        }

        var previousStatus = job.getStatus();
        JobStateMachine.validateTransition(previousStatus, JobStatus.IN_PROGRESS);

        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        var acceptedProposals = proposalRepository.findByJobIdAndStatus(event.jobId(), ProposalStatus.ACCEPTED);
        if (!acceptedProposals.isEmpty()) {
            var acceptedProposal = acceptedProposals.get(0);
            var contract = com.unikly.jobservice.domain.Contract.builder()
                    .jobId(job.getId())
                    .clientId(job.getClientId())
                    .freelancerId(acceptedProposal.getFreelancerId())
                    .agreedBudget(acceptedProposal.getProposedBudget())
                    .status(ContractStatus.ACTIVE)
                    .build();
            contractRepository.save(contract);
        }

        publishStatusChangedEvent(job, previousStatus);

        log.info("Job transitioned to IN_PROGRESS after payment: jobId={}", event.jobId());

        markProcessed(event.eventId());
    }

    @Transactional
    public void handleEscrowReleased(EscrowReleasedEvent event) {
        if (isAlreadyProcessed(event.eventId())) {
            log.info("Duplicate EscrowReleasedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        log.info("EscrowReleasedEvent received: jobId={}, freelancerId={}, amount={} {}",
                event.jobId(), event.freelancerId(), event.amount().amount(), event.amount().currency());

        markProcessed(event.eventId());
    }

    private boolean isAlreadyProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void markProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }

    private void publishStatusChangedEvent(com.unikly.jobservice.domain.Job job, JobStatus previousStatus) {
        try {
            var event = new JobStatusChangedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    job.getId(), previousStatus.name(), job.getStatus().name(), job.getClientId()
            );
            outboxRepository.save(new OutboxEvent(event.eventType(), job.getId(), "JOB", objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish JobStatusChangedEvent for job={}", job.getId(), e);
        }
    }
}
