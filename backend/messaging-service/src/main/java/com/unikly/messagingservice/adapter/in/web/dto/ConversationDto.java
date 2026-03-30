package com.unikly.messagingservice.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        UUID jobId,
        List<UUID> participantIds,
        Instant createdAt,
        Instant lastMessageAt,
        long unreadCount,
        String lastMessagePreview
) {}
