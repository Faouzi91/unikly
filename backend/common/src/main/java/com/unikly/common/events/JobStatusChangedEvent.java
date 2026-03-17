package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record JobStatusChangedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        String previousStatus,
        String newStatus,
        UUID triggeredBy
) implements BaseEvent {

    public JobStatusChangedEvent {
        if (eventType == null) eventType = "JobStatusChanged";
    }
}
