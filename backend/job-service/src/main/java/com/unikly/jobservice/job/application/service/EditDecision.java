package com.unikly.jobservice.job.application.service;

import java.util.List;

public record EditDecision(
        boolean allowed,
        boolean requiresConfirmation,
        List<String> sensitiveFieldsChanged,
        ProposalImpact proposalImpact,
        String message
) {
    public enum ProposalImpact {
        NONE, NEEDS_REVIEW, OUTDATED
    }
}
