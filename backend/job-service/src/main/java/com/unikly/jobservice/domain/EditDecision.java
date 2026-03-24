package com.unikly.jobservice.domain;

import java.util.List;

public record EditDecision(
        boolean allowed,
        boolean requiresConfirmation,
        List<String> sensitiveFieldsChanged,
        ProposalImpact proposalImpact,
        int activeProposalCount,
        String message
) {}
