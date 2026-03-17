package com.unikly.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileUpdatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID userId,
        List<String> updatedFields,
        List<String> skills
) implements BaseEvent {

    public UserProfileUpdatedEvent {
        if (eventType == null) eventType = "UserProfileUpdated";
    }
}
