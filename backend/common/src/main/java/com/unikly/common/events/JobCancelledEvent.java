package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record JobCancelledEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId
) implements BaseEvent {
    public JobCancelledEvent {
        if (eventType == null) eventType = "JobCancelled";
    }
}
