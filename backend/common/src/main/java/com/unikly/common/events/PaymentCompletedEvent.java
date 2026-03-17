package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID paymentId,
        UUID jobId,
        String providerRef
) implements BaseEvent {

    public PaymentCompletedEvent {
        if (eventType == null) eventType = "PaymentCompleted";
    }
}
