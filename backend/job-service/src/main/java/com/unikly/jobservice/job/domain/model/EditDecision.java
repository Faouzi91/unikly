package com.unikly.jobservice.job.domain.model;

import java.util.List;
import com.unikly.jobservice.proposal.domain.model.ProposalImpact;

public record EditDecision(
        boolean allowed,
        boolean requiresConfirmation,
        List<String> sensitiveFieldsChanged,
        ProposalImpact proposalImpact,
        int activeProposalCount,
        String message
) {}
