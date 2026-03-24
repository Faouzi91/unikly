package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record JobUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId
) implements BaseEvent {
    public JobUpdatedEvent {
        if (eventType == null) eventType = "JobUpdated";
    }
}
