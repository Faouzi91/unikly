package com.unikly.common.events;

import com.unikly.common.dto.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID paymentId,
        UUID jobId,
        UUID clientId,
        Money amount,
        String providerRef
) implements BaseEvent {

    public PaymentInitiatedEvent {
        if (eventType == null) eventType = "PaymentInitiated";
    }
}
