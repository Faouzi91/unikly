package com.unikly.common.events;

import com.unikly.common.dto.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId,
        String title,
        String description,
        List<String> skills,
        Money budget,
        String status
) implements BaseEvent {

    public JobCreatedEvent {
        if (eventType == null) eventType = "JobCreated";
    }
}
