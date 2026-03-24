package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record JobPublishedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId
) implements BaseEvent {
    public JobPublishedEvent {
        if (eventType == null) eventType = "JobPublished";
    }
}
