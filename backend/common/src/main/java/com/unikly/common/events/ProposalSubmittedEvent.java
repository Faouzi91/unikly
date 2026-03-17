package com.unikly.common.events;

import com.unikly.common.dto.Money;

import java.time.Instant;
import java.util.UUID;

public record ProposalSubmittedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID freelancerId,
        Money proposedBudget
) implements BaseEvent {

    public ProposalSubmittedEvent {
        if (eventType == null) eventType = "ProposalSubmitted";
    }
}
