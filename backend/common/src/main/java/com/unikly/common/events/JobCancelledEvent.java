package com.unikly.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobCancelledEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId,
        List<UUID> affectedFreelancerIds
) implements BaseEvent {
    public JobCancelledEvent {
        if (eventType == null) eventType = "JobCancelled";
        if (affectedFreelancerIds == null) affectedFreelancerIds = List.of();
    }
}
