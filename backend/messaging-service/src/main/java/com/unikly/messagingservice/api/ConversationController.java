package com.unikly.messagingservice.api;

import com.unikly.messagingservice.application.ConversationDto;
import com.unikly.messagingservice.application.ConversationService;
import com.unikly.messagingservice.application.GetOrCreateConversationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * GET /api/v1/messages/conversations?page=0
     * Returns paginated conversations for the authenticated user, sorted by lastMessageAt DESC.
     */
    @GetMapping
    public ResponseEntity<Page<ConversationDto>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getUserConversations(userId, page));
    }

    /**
     * GET /api/v1/messages/conversations/{id}
     * Returns conversation details (participant check enforced).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getById(id, userId));
    }

    /**
     * POST /api/v1/messages/conversations
     * Find or create a conversation between two participants (optionally linked to a job).
     */
    @PostMapping
    public ResponseEntity<ConversationDto> getOrCreate(
            @Valid @RequestBody GetOrCreateConversationRequest request) {

        return ResponseEntity.ok(conversationService.getOrCreateConversation(request));
    }
}
