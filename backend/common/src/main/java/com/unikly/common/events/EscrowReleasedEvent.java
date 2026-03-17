package com.unikly.common.events;

import com.unikly.common.dto.Money;

import java.time.Instant;
import java.util.UUID;

public record EscrowReleasedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID paymentId,
        UUID jobId,
        UUID freelancerId,
        Money amount
) implements BaseEvent {

    public EscrowReleasedEvent {
        if (eventType == null) eventType = "EscrowReleased";
    }
}
