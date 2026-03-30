package com.unikly.jobservice.job.application;

import com.unikly.jobservice.job.adapter.in.web.dto.UpdateJobRequest;
import com.unikly.jobservice.job.domain.model.EditDecision;
import com.unikly.jobservice.job.domain.model.Job;
import com.unikly.jobservice.job.domain.model.JobStatus;
import com.unikly.jobservice.proposal.domain.model.ProposalImpact;
import com.unikly.jobservice.proposal.domain.model.ProposalStatus;
import com.unikly.jobservice.proposal.application.port.out.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobEditRulesEngine {

    private final ProposalRepository proposalRepository;

    public EditDecision evaluateEdit(Job job, UpdateJobRequest updateRequest) {
        JobStatus status = job.getStatus();

        if (status == JobStatus.IN_REVIEW
                || status == JobStatus.IN_PROGRESS
                || status == JobStatus.COMPLETED
                || status == JobStatus.CLOSED) {
            return new EditDecision(false, false, List.of(), ProposalImpact.NONE, 0,
                    "Job cannot be edited in status " + status);
        }

        if (status == JobStatus.DRAFT) {
            return new EditDecision(true, false, List.of(), ProposalImpact.NONE, 0,
                    "Draft jobs can be freely edited");
        }

        // OPEN
        long count = proposalRepository.countByJobIdAndStatusNotIn(
                job.getId(),
                List.of(ProposalStatus.REJECTED, ProposalStatus.WITHDRAWN, ProposalStatus.OUTDATED));

        List<String> sensitiveChanged = new ArrayList<>();
        if (updateRequest.budget() != null && !updateRequest.budget().equals(job.getBudget())) {
            sensitiveChanged.add("budget");
        }
        if (updateRequest.skills() != null && !updateRequest.skills().equals(job.getSkills())) {
            sensitiveChanged.add("skills");
        }
        if (updateRequest.title() != null && !updateRequest.title().equals(job.getTitle())) {
            sensitiveChanged.add("title");
        }

        if (count == 0) {
            return new EditDecision(true, false, sensitiveChanged, ProposalImpact.NONE, 0,
                    "No active proposals — edit is safe");
        }

        if (sensitiveChanged.isEmpty()) {
            return new EditDecision(true, false, List.of(), ProposalImpact.NONE, (int) count,
                    "No sensitive fields changed — proposals unaffected");
        }

        return new EditDecision(true, true, sensitiveChanged, ProposalImpact.OUTDATED, (int) count,
                "This job has " + count + " active proposals. Changing "
                        + sensitiveChanged + " may invalidate them.");
    }
}
