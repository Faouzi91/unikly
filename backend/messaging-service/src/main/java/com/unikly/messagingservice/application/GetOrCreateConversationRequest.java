package com.unikly.messagingservice.application;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GetOrCreateConversationRequest(
        @NotEmpty @Size(min = 2, max = 2) List<UUID> participantIds,
        UUID jobId
) {}
