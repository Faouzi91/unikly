package com.unikly.messagingservice.api;

import com.unikly.messagingservice.application.MessageDto;
import com.unikly.messagingservice.application.MessageService;
import com.unikly.messagingservice.application.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/v1/messages/conversations/{id}?page=0&size=50
     * Returns paginated messages for a conversation (oldest first within page).
     */
    @GetMapping("/api/v1/messages/conversations/{id}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(messageService.getMessages(id, userId, page, size));
    }

    /**
     * POST /api/v1/messages/conversations/{id}
     * Send a message to a conversation.
     */
    @PostMapping("/api/v1/messages/conversations/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID senderId = UUID.fromString(jwt.getSubject());
        return messageService.sendMessage(id, senderId, request.content(), request.contentType());
    }

    /**
     * PATCH /api/v1/messages/{messageId}/read
     * Mark a message as read by the authenticated user.
     */
    @PatchMapping("/api/v1/messages/{messageId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        messageService.markAsRead(messageId, userId);
    }
}
