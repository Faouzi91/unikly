package com.unikly.jobservice.application.service;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.events.ProposalAcceptedEvent;
import com.unikly.common.events.ProposalSubmittedEvent;
import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.application.mapper.ProposalMapper;
import com.unikly.jobservice.domain.DuplicateProposalException;
import com.unikly.jobservice.domain.InvalidProposalStateException;
import com.unikly.jobservice.domain.Proposal;
import com.unikly.jobservice.domain.ProposalStatus;
import com.unikly.jobservice.domain.JobStatus;
import com.unikly.jobservice.infrastructure.repository.ContractRepository;
import com.unikly.jobservice.infrastructure.repository.JobRepository;
import com.unikly.jobservice.infrastructure.repository.ProposalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final ContractRepository contractRepository;
    private final ProposalMapper proposalMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public ProposalResponse submitProposal(UUID jobId, UUID freelancerId, SubmitProposalRequest request) {
        log.info("Freelancer {} submitting proposal for job {}", freelancerId, jobId);

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new InvalidProposalStateException("Job is not accepting proposals: " + job.getStatus());
        }

        if (proposalRepository.existsByJobIdAndFreelancerId(jobId, freelancerId)) {
            throw new DuplicateProposalException("Proposal already submitted for job: " + jobId);
        }

        Proposal proposal = proposalMapper.toEntity(request);
        proposal.setJobId(jobId);
        proposal.setFreelancerId(freelancerId);
        proposal = proposalRepository.save(proposal);

        var event = new ProposalSubmittedEvent(
                UUID.randomUUID(), "ProposalSubmitted", Instant.now(),
                jobId, freelancerId,
                new com.unikly.common.dto.Money(request.proposedBudget(), job.getCurrency())
        );
        outboxEventPublisher.publish("PROPOSAL", proposal.getId(), event);

        log.info("Proposal {} submitted for job {}", proposal.getId(), jobId);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProposalResponse> listProposals(UUID jobId, UUID clientId, int page, int size) {
        log.info("Client {} listing proposals for job {}", clientId, jobId);

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new SecurityException("You do not own this job");
        }

        var pageable = PageRequest.of(page, size);
        var proposals = proposalRepository.findByJobId(jobId, pageable);

        return new PageResponse<>(
                proposals.getContent().stream().map(proposalMapper::toResponse).toList(),
                proposals.getNumber(),
                proposals.getSize(),
                proposals.getTotalElements(),
                proposals.getTotalPages()
        );
    }

    @Transactional
    public ProposalResponse acceptProposal(UUID jobId, UUID proposalId, UUID clientId) {
        log.info("Client {} accepting proposal {} for job {}", clientId, proposalId, jobId);

        var proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + proposalId));

        if (!proposal.getJobId().equals(jobId)) {
            throw new InvalidProposalStateException("Proposal does not belong to job: " + jobId);
        }

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new SecurityException("You do not own this job");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new InvalidProposalStateException("Only PENDING proposals can be accepted");
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposalRepository.save(proposal);

        proposalRepository.rejectOtherPendingProposals(jobId, proposalId, ProposalStatus.REJECTED);

        var event = new ProposalAcceptedEvent(
                UUID.randomUUID(), "ProposalAccepted", Instant.now(),
                jobId, proposal.getFreelancerId(), clientId,
                new com.unikly.common.dto.Money(proposal.getProposedBudget(), job.getCurrency())
        );
        outboxEventPublisher.publish("PROPOSAL", proposal.getId(), event);

        log.info("Proposal {} accepted for job {}", proposalId, jobId);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public ProposalResponse rejectProposal(UUID jobId, UUID proposalId, UUID clientId) {
        log.info("Client {} rejecting proposal {} for job {}", clientId, proposalId, jobId);

        var proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + proposalId));

        if (!proposal.getJobId().equals(jobId)) {
            throw new InvalidProposalStateException("Proposal does not belong to job: " + jobId);
        }

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new SecurityException("You do not own this job");
        }

        if (proposal.getStatus() == ProposalStatus.ACCEPTED) {
            throw new InvalidProposalStateException("Cannot reject an already accepted proposal");
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        proposalRepository.save(proposal);

        log.info("Proposal {} rejected for job {}", proposalId, jobId);
        return proposalMapper.toResponse(proposal);
    }
}
