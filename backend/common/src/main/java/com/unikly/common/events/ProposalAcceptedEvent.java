package com.unikly.common.events;

import com.unikly.common.dto.Money;

import java.time.Instant;
import java.util.UUID;

public record ProposalAcceptedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID freelancerId,
        UUID clientId,
        Money agreedBudget
) implements BaseEvent {

    public ProposalAcceptedEvent {
        if (eventType == null) eventType = "ProposalAccepted";
    }
}
