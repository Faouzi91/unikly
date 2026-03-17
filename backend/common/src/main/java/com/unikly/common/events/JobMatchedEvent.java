package com.unikly.common.events;

import com.unikly.common.dto.MatchEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobMatchedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        List<MatchEntry> matches,
        String strategy
) implements BaseEvent {

    public JobMatchedEvent {
        if (eventType == null) eventType = "JobMatched";
    }
}
