package com.unikly.jobservice.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.EscrowReleasedEvent;
import com.unikly.common.events.JobStatusChangedEvent;
import com.unikly.common.events.PaymentCompletedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.domain.JobStatusMachine;
import com.unikly.jobservice.domain.ProcessedEvent;
import com.unikly.jobservice.domain.ProposalStatus;
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
        var acceptedProposals = proposalRepository.findByJobIdAndStatus(event.jobId(), ProposalStatus.ACCEPTED);

        if (job.getStatus() == JobStatus.OPEN && !acceptedProposals.isEmpty()) {
            var acceptedProposal = acceptedProposals.get(0);
            var previousStatus = job.getStatus();
            JobStatusMachine.validateTransition(previousStatus, JobStatus.IN_PROGRESS);

            job.setStatus(JobStatus.IN_PROGRESS);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            // Create and activate the contract now that payment is confirmed
            var contract = com.unikly.jobservice.domain.Contract.builder()
                    .jobId(job.getId())
                    .clientId(job.getClientId())
                    .freelancerId(acceptedProposal.getFreelancerId())
                    .agreedBudget(acceptedProposal.getProposedBudget())
                    .status(com.unikly.jobservice.domain.ContractStatus.ACTIVE)
                    .startedAt(Instant.now())
                    .build();
            contractRepository.save(contract);

            publishStatusChangedEvent(job, previousStatus);

            log.info("Job transitioned to IN_PROGRESS and contract created via PaymentCompletedEvent: jobId={}, paymentId={}",
                    event.jobId(), event.paymentId());
        } else {
            log.info("Job not eligible for transition: jobId={}, status={}, acceptedProposals={}",
                    event.jobId(), job.getStatus(), acceptedProposals.size());
        }

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
            outboxRepository.save(new OutboxEvent(event.eventType(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish JobStatusChangedEvent for job={}", job.getId(), e);
        }
    }
}
