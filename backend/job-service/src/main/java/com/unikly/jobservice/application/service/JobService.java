package com.unikly.jobservice.application.service;

import com.unikly.common.dto.Money;
import com.unikly.common.events.JobCancelledEvent;
import com.unikly.common.events.JobCreatedEvent;
import com.unikly.common.events.JobPublishedEvent;
import com.unikly.common.events.JobUpdatedEvent;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.JobResponse;
import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.application.JobEditRulesEngine;
import com.unikly.jobservice.application.mapper.JobMapper;
import com.unikly.jobservice.domain.EditConfirmationRequiredException;
import com.unikly.jobservice.domain.EditDecision;
import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobNotEditableException;
import com.unikly.jobservice.domain.JobStateMachine;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.domain.ProposalImpact;
import com.unikly.jobservice.domain.ProposalStatus;
import com.unikly.jobservice.infrastructure.repository.ContractRepository;
import com.unikly.jobservice.infrastructure.repository.JobRepository;
import com.unikly.jobservice.infrastructure.repository.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ProposalRepository proposalRepository;
    private final ContractRepository contractRepository;
    private final JobMapper jobMapper;
    private final JobEditRulesEngine jobEditRulesEngine;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public JobResponse createJob(UUID clientId, CreateJobRequest request) {
        log.info("Creating draft job for client {}", clientId);
        Job job = jobMapper.toEntity(request);
        job.setClientId(clientId);
        job.setStatus(JobStatus.DRAFT);
        job = jobRepository.save(job);

        var event = new JobCreatedEvent(
                UUID.randomUUID(), "JobCreated", Instant.now(), job.getId(), clientId,
                job.getTitle(), job.getDescription(), job.getSkills(),
                new Money(job.getBudget(), job.getCurrency()), job.getStatus().name()
        );
        outboxEventPublisher.publish("JOB", job.getId(), event);

        return jobMapper.toResponse(job);
    }

    @Transactional
    public JobResponse publishJob(UUID jobId, UUID clientId) {
        log.info("Publishing job {} for client {}", jobId, clientId);
        Job job = getJobOrThrow(jobId);
        validateOwnership(job, clientId);
        JobStateMachine.validateTransition(job.getStatus(), JobStatus.OPEN);

        job.setStatus(JobStatus.OPEN);
        job = jobRepository.save(job);

        var event = new JobPublishedEvent(UUID.randomUUID(), "JobPublished", Instant.now(), job.getId(), clientId);
        outboxEventPublisher.publish("JOB", job.getId(), event);

        return jobMapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        log.info("Fetching job {}", jobId);
        return jobMapper.toResponse(getJobOrThrow(jobId));
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByClient(UUID clientId, Pageable pageable) {
        log.info("Fetching jobs for client {}", clientId);
        return jobRepository.findByClientId(clientId, pageable).map(jobMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EditDecision checkEditEligibility(UUID jobId, UUID clientId, UpdateJobRequest request) {
        log.info("Checking edit eligibility for job {} by client {}", jobId, clientId);
        Job job = findJobOwnedBy(jobId, clientId);
        return jobEditRulesEngine.evaluateEdit(job, request);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, UUID clientId, UpdateJobRequest request, boolean confirmed) {
        log.info("Updating job {} by client {}, confirmed={}", jobId, clientId, confirmed);
        Job job = findJobOwnedBy(jobId, clientId);
        EditDecision decision = jobEditRulesEngine.evaluateEdit(job, request);

        if (!decision.allowed()) {
            throw new JobNotEditableException(decision.message());
        }
        if (decision.requiresConfirmation() && !confirmed) {
            throw new EditConfirmationRequiredException(decision);
        }

        jobMapper.updateEntity(request, job);
        Job saved = jobRepository.save(job);

        if (decision.proposalImpact() == ProposalImpact.OUTDATED) {
            proposalRepository.bulkUpdateStatusByJobId(
                    jobId,
                    List.of(ProposalStatus.SUBMITTED, ProposalStatus.PENDING,
                            ProposalStatus.VIEWED, ProposalStatus.SHORTLISTED),
                    ProposalStatus.OUTDATED
            );
        }

        outboxEventPublisher.publish("JOB", saved.getId(),
                new JobUpdatedEvent(UUID.randomUUID(), "JobUpdated", Instant.now(),
                        saved.getId(), clientId, decision.sensitiveFieldsChanged(), saved.getVersion()));

        log.info("Job updated: id={}, version={}, sensitiveChanges={}",
                saved.getId(), saved.getVersion(), decision.sensitiveFieldsChanged());

        return jobMapper.toResponse(saved);
    }

    @Transactional
    public void cancelJob(UUID jobId, UUID clientId) {
        log.info("Cancelling job {} by client {}", jobId, clientId);
        Job job = findJobOwnedBy(jobId, clientId);
        JobStateMachine.validateTransition(job.getStatus(), JobStatus.CANCELLED);

        proposalRepository.bulkUpdateStatusByJobId(
                jobId,
                List.of(ProposalStatus.SUBMITTED, ProposalStatus.PENDING,
                        ProposalStatus.VIEWED, ProposalStatus.SHORTLISTED),
                ProposalStatus.REJECTED
        );

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);

        outboxEventPublisher.publish("JOB", jobId,
                new JobCancelledEvent(UUID.randomUUID(), "JobCancelled", Instant.now(), jobId, clientId));

        log.info("Job cancelled: id={}", jobId);
    }

    @Transactional
    public JobResponse transitionStatus(UUID jobId, UUID userId, JobStatus targetStatus) {
        log.info("Transitioning job {} to {} by user {}", jobId, targetStatus, userId);
        Job job = getJobOrThrow(jobId);
        validateOwnership(job, userId);
        JobStateMachine.validateTransition(job.getStatus(), targetStatus);

        job.setStatus(targetStatus);
        job = jobRepository.save(job);

        if (targetStatus == JobStatus.OPEN) {
            var event = new JobPublishedEvent(UUID.randomUUID(), "JobPublished", Instant.now(), job.getId(), userId);
            outboxEventPublisher.publish("JOB", job.getId(), event);
        } else if (targetStatus == JobStatus.CANCELLED) {
            proposalRepository.bulkUpdateStatusByJobId(
                    jobId,
                    List.of(ProposalStatus.SUBMITTED, ProposalStatus.PENDING,
                            ProposalStatus.VIEWED, ProposalStatus.SHORTLISTED),
                    ProposalStatus.REJECTED
            );
            var event = new JobCancelledEvent(UUID.randomUUID(), "JobCancelled", Instant.now(), job.getId(), userId);
            outboxEventPublisher.publish("JOB", job.getId(), event);
        }

        return jobMapper.toResponse(job);
    }

    private Job findJobOwnedBy(UUID jobId, UUID clientId) {
        Job job = getJobOrThrow(jobId);
        validateOwnership(job, clientId);
        return job;
    }

    private Job getJobOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));
    }

    private void validateOwnership(Job job, UUID clientId) {
        if (!job.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("You do not own this job");
        }
    }
}
