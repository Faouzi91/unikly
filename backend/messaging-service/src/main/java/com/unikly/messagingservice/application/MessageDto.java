package com.unikly.messagingservice.application;

import com.unikly.messagingservice.domain.MessageContentType;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String content,
        MessageContentType contentType,
        Instant readAt,
        Instant createdAt
) {}
