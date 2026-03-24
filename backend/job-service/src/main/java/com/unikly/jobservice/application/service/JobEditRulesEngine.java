package com.unikly.jobservice.application.service;

import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobNotEditableException;
import com.unikly.jobservice.domain.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobEditRulesEngine {

    public EditDecision evaluateEdit(Job job, UpdateJobRequest request, long activeProposalCount) {
        log.debug("Evaluating edit rules for job {}, status {}, proposals {}", job.getId(), job.getStatus(), activeProposalCount);

        if (job.getStatus() == JobStatus.DRAFT) {
            return new EditDecision(true, false, List.of(), EditDecision.ProposalImpact.NONE, "Drafts can be edited freely.");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new JobNotEditableException("Job in status " + job.getStatus() + " cannot be edited.");
        }

        List<String> sensitiveFields = detectSensitiveChanges(job, request);

        if (activeProposalCount == 0) {
            return new EditDecision(true, false, sensitiveFields, EditDecision.ProposalImpact.NONE, "Open job with no proposals can be edited freely.");
        }

        // OPEN with proposals
        if (sensitiveFields.isEmpty()) {
            return new EditDecision(true, false, List.of(), EditDecision.ProposalImpact.NONE, "Safe changes applied without affecting proposals.");
        } else {
            return new EditDecision(true, true, sensitiveFields, EditDecision.ProposalImpact.OUTDATED, "Sensitive changes require confirmation and will mark proposals as OUTDATED.");
        }
    }

    private List<String> detectSensitiveChanges(Job job, UpdateJobRequest request) {
        List<String> sensitiveFields = new ArrayList<>();

        if (request.budget() != null && request.budget().compareTo(job.getBudget()) != 0) {
            sensitiveFields.add("budget");
        }
        if (request.skills() != null && !request.skills().equals(job.getSkills())) {
            sensitiveFields.add("skills");
        }
        if (request.title() != null && !request.title().equals(job.getTitle())) {
            sensitiveFields.add("title");
        }

        return sensitiveFields;
    }
}
