package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID conversationId,
        UUID senderId,
        UUID recipientId,
        String preview
) implements BaseEvent {

    public MessageSentEvent {
        if (eventType == null) eventType = "MessageSent";
    }
}
