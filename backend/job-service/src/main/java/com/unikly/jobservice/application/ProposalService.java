package com.unikly.jobservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.Money;
import com.unikly.common.dto.PageResponse;
import com.unikly.common.events.ProposalAcceptedEvent;
import com.unikly.common.events.ProposalSubmittedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.api.mapper.ProposalMapper;
import com.unikly.jobservice.domain.Contract;
import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.domain.JobStatusMachine;
import com.unikly.jobservice.domain.Proposal;
import com.unikly.jobservice.domain.ProposalStatus;
import com.unikly.jobservice.infrastructure.ContractRepository;
import com.unikly.jobservice.infrastructure.JobRepository;
import com.unikly.jobservice.infrastructure.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final ContractRepository contractRepository;
    private final OutboxRepository outboxRepository;
    private final ProposalMapper proposalMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProposalResponse submitProposal(UUID jobId, UUID freelancerId, SubmitProposalRequest request) {
        var job = findJobOrThrow(jobId);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("Proposals can only be submitted to OPEN jobs");
        }
        if (job.getClientId().equals(freelancerId)) {
            throw new IllegalArgumentException("You cannot submit a proposal to your own job");
        }

        var proposal = Proposal.builder()
                .jobId(jobId)
                .freelancerId(freelancerId)
                .proposedBudget(request.proposedBudget())
                .coverLetter(request.coverLetter())
                .status(ProposalStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        proposal = proposalRepository.save(proposal);
        publishProposalSubmittedEvent(job, proposal);

        log.info("Proposal submitted: id={}, jobId={}, freelancerId={}", proposal.getId(), jobId, freelancerId);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public ProposalResponse acceptProposal(UUID jobId, UUID proposalId, UUID clientId) {
        var job = findJobOrThrow(jobId);
        validateOwnership(job, clientId);

        var proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + proposalId));

        if (!proposal.getJobId().equals(jobId)) {
            throw new IllegalArgumentException("Proposal does not belong to this job");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING proposals can be accepted");
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposalRepository.save(proposal);

        proposalRepository.rejectOtherPendingProposals(jobId, proposalId, ProposalStatus.REJECTED);

        JobStatusMachine.validateTransition(job.getStatus(), JobStatus.IN_PROGRESS);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        var contract = Contract.builder()
                .jobId(jobId)
                .clientId(clientId)
                .freelancerId(proposal.getFreelancerId())
                .agreedBudget(proposal.getProposedBudget())
                .status(com.unikly.jobservice.domain.ContractStatus.ACTIVE)
                .startedAt(Instant.now())
                .build();
        contractRepository.save(contract);

        publishProposalAcceptedEvent(job, proposal);

        log.info("Proposal accepted: id={}, jobId={}, contract created", proposalId, jobId);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public ProposalResponse rejectProposal(UUID jobId, UUID proposalId, UUID clientId) {
        var job = findJobOrThrow(jobId);
        validateOwnership(job, clientId);

        var proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + proposalId));

        if (!proposal.getJobId().equals(jobId)) {
            throw new IllegalArgumentException("Proposal does not belong to this job");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING proposals can be rejected");
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        proposalRepository.save(proposal);

        log.info("Proposal rejected: id={}, jobId={}", proposalId, jobId);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProposalResponse> listProposals(UUID jobId, UUID clientId, int page, int size) {
        var job = findJobOrThrow(jobId);
        validateOwnership(job, clientId);

        var result = proposalRepository.findByJobId(jobId, PageRequest.of(page, size));
        var content = result.getContent().stream()
                .map(proposalMapper::toResponse)
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private Job findJobOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));
    }

    private void validateOwnership(Job job, UUID clientId) {
        if (!job.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("You are not the owner of this job");
        }
    }

    private void publishProposalSubmittedEvent(Job job, Proposal proposal) {
        try {
            var event = new ProposalSubmittedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    job.getId(), proposal.getFreelancerId(),
                    new Money(proposal.getProposedBudget(), job.getCurrency())
            );
            outboxRepository.save(new OutboxEvent(event.eventType(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish ProposalSubmittedEvent", e);
        }
    }

    private void publishProposalAcceptedEvent(Job job, Proposal proposal) {
        try {
            var event = new ProposalAcceptedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    job.getId(), proposal.getFreelancerId(), job.getClientId(),
                    new Money(proposal.getProposedBudget(), job.getCurrency())
            );
            outboxRepository.save(new OutboxEvent(event.eventType(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.error("Failed to publish ProposalAcceptedEvent", e);
        }
    }
}
