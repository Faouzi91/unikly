package com.unikly.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId,
        List<String> changedFields,
        int newVersion,
        List<UUID> affectedFreelancerIds
) implements BaseEvent {
    public JobUpdatedEvent {
        if (eventType == null) eventType = "JobUpdated";
        if (affectedFreelancerIds == null) affectedFreelancerIds = List.of();
    }
}
