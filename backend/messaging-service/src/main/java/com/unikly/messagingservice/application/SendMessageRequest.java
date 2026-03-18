package com.unikly.messagingservice.application;

import com.unikly.messagingservice.domain.MessageContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 10_000) String content,
        @NotNull MessageContentType contentType
) {}
