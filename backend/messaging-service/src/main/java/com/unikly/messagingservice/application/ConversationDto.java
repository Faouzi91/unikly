package com.unikly.messagingservice.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        UUID jobId,
        List<UUID> participantIds,
        Instant createdAt,
        Instant lastMessageAt
) {}
