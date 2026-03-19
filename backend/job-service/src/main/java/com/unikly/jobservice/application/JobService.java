package com.unikly.jobservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.Money;
import com.unikly.common.dto.PageResponse;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobStatusChangedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.JobResponse;
import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.api.mapper.JobMapper;
import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.domain.JobStatusMachine;
import com.unikly.jobservice.infrastructure.JobRepository;
import com.unikly.jobservice.infrastructure.JobSpecifications;
import com.unikly.jobservice.infrastructure.ProposalRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ProposalRepository proposalRepository;
    private final OutboxRepository outboxRepository;
    private final JobMapper jobMapper;
    private final ObjectMapper objectMapper;

    private final Counter jobCreatedCounter;
    private final Counter proposalSubmittedCounter;
    private final Timer jobStatusTransitionTimer;

    public JobService(JobRepository jobRepository,
                      ProposalRepository proposalRepository,
                      OutboxRepository outboxRepository,
                      JobMapper jobMapper,
                      ObjectMapper objectMapper,
                      MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.proposalRepository = proposalRepository;
        this.outboxRepository = outboxRepository;
        this.jobMapper = jobMapper;
        this.objectMapper = objectMapper;

        this.jobCreatedCounter = Counter.builder("unikly_job_created_total")
                .description("Total number of jobs created")
                .tag("status", "OPEN")
                .register(meterRegistry);
        this.proposalSubmittedCounter = Counter.builder("unikly_proposal_submitted_total")
                .description("Total number of proposals submitted")
                .register(meterRegistry);
        this.jobStatusTransitionTimer = Timer.builder("unikly_job_status_transition_seconds")
                .description("Time taken for job status transitions")
                .register(meterRegistry);
    }

    @Transactional
    public JobResponse createJob(UUID clientId, CreateJobRequest request) {
        var now = Instant.now();
        var job = Job.builder()
                .clientId(clientId)
                .title(request.title())
                .description(request.description())
                .budget(request.budget())
                .currency(request.currency())
                .skills(request.skills())
                .status(JobStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        job = jobRepository.save(job);
        log.info("Job created: id={}, clientId={}", job.getId(), clientId);
        return jobMapper.toResponse(job, 0);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id) {
        var job = findJobOrThrow(id);
        long proposalCount = proposalRepository.countByJobId(id);
        return jobMapper.toResponse(job, proposalCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> listJobs(JobStatus status, String skill,
                                               BigDecimal minBudget, BigDecimal maxBudget,
                                               String sortField, String sortDir,
                                               int page, int size) {
        var spec = Specification.where(JobSpecifications.hasStatus(status))
                .and(JobSpecifications.hasSkill(skill))
                .and(JobSpecifications.minBudget(minBudget))
                .and(JobSpecifications.maxBudget(maxBudget));

        var direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var sort = Sort.by(direction, sortField != null ? sortField : "createdAt");
        var pageable = PageRequest.of(page, size, sort);

        var result = jobRepository.findAll(spec, pageable);
        var content = result.getContent().stream()
                .map(job -> jobMapper.toResponse(job, proposalRepository.countByJobId(job.getId())))
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public JobResponse updateJob(UUID id, UUID clientId, UpdateJobRequest request) {
        var job = findJobOrThrow(id);
        validateOwnership(job, clientId);

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new IllegalStateException("Job can only be updated in DRAFT status");
        }

        if (request.title() != null) job.setTitle(request.title());
        if (request.description() != null) job.setDescription(request.description());
        if (request.budget() != null) job.setBudget(request.budget());
        if (request.currency() != null) job.setCurrency(request.currency());
        if (request.skills() != null) job.setSkills(request.skills());
        job.setUpdatedAt(Instant.now());

        job = jobRepository.save(job);
        log.info("Job updated: id={}", id);
        return jobMapper.toResponse(job, proposalRepository.countByJobId(id));
    }

    @Transactional
    public JobResponse transitionStatus(UUID id, UUID userId, JobStatus newStatus) {
        var job = findJobOrThrow(id);
        validateOwnership(job, userId);

        var previousStatus = job.getStatus();
        JobStatusMachine.validateTransition(previousStatus, newStatus);

        var sample = Timer.start();

        job.setStatus(newStatus);
        job.setUpdatedAt(Instant.now());
        job = jobRepository.save(job);

        publishStatusChangedEvent(job, previousStatus, userId);

        if (previousStatus == JobStatus.DRAFT && newStatus == JobStatus.OPEN) {
            publishJobCreatedEvent(job);
            jobCreatedCounter.increment();
        }

        sample.stop(jobStatusTransitionTimer);
        log.info("Job status transitioned: id={}, {} → {}", id, previousStatus, newStatus);
        return jobMapper.toResponse(job, proposalRepository.countByJobId(id));
    }

    public void recordProposalSubmitted() {
        proposalSubmittedCounter.increment();
    }

    private Job findJobOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
    }

    private void validateOwnership(Job job, UUID userId) {
        if (!job.getClientId().equals(userId)) {
            throw new IllegalArgumentException("You are not the owner of this job");
        }
    }

    private void publishJobCreatedEvent(Job job) {
        try {
            var event = new JobCreatedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    job.getId(), job.getClientId(), job.getTitle(), job.getDescription(),
                    job.getSkills(), new Money(job.getBudget(), job.getCurrency()),
                    job.getStatus().name()
            );
            outboxRepository.save(new OutboxEvent(event.eventType(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish JobCreatedEvent for job={}", job.getId(), e);
        }
    }

    private void publishStatusChangedEvent(Job job, JobStatus previousStatus, UUID triggeredBy) {
        try {
            var event = new JobStatusChangedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    job.getId(), previousStatus.name(), job.getStatus().name(), triggeredBy
            );
            outboxRepository.save(new OutboxEvent(event.eventType(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish JobStatusChangedEvent for job={}", job.getId(), e);
        }
    }
}
