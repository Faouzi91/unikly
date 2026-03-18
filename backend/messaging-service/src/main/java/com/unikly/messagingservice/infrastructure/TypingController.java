package com.unikly.messagingservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Handles ephemeral typing indicator frames.
 * Client sends: SEND /app/typing {"conversationId":"...", "recipientId":"..."}
 * Server forwards: /user/{recipientId}/queue/typing
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TypingController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/typing")
    public void handleTyping(TypingPayload payload, Principal principal) {
        if (principal == null || payload.recipientId() == null) return;

        messagingTemplate.convertAndSendToUser(
                payload.recipientId(),
                "/queue/typing",
                new TypingIndicator(payload.conversationId(), principal.getName())
        );
    }

    public record TypingPayload(String conversationId, String recipientId) {}

    public record TypingIndicator(String conversationId, String senderId) {}
}
