package com.unikly.messagingservice.adapter.in.web;

import com.unikly.messagingservice.adapter.in.web.dto.ConversationDto;
import com.unikly.messagingservice.application.service.ConversationService;
import com.unikly.messagingservice.adapter.in.web.dto.GetOrCreateConversationRequest;
import com.unikly.messagingservice.adapter.in.websocket.MessagingPresenceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Conversations", description = "Messaging conversation management")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessagingPresenceManager presenceManager;

    @GetMapping
    @Operation(summary = "List conversations", description = "Returns paginated conversations for the authenticated user, sorted by lastMessageAt DESC")
    @ApiResponse(responseCode = "200", description = "Conversations retrieved")
    public ResponseEntity<Page<ConversationDto>> getConversations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getUserConversations(userId, page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation by ID")
    @ApiResponse(responseCode = "200", description = "Conversation retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden — not a participant")
    @ApiResponse(responseCode = "404", description = "Conversation not found")
    public ResponseEntity<ConversationDto> getConversation(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getById(id, userId));
    }

    @PostMapping
    @Operation(summary = "Get or create a conversation", description = "Find an existing conversation between two participants or create one, optionally linked to a job")
    @ApiResponse(responseCode = "200", description = "Conversation found or created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<ConversationDto> getOrCreate(
            @Valid @RequestBody GetOrCreateConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getOrCreateConversation(request, userId));
    }

    @GetMapping("/presence")
    @Operation(summary = "Check online status of users", description = "Returns a map of userId → online boolean")
    @ApiResponse(responseCode = "200", description = "Presence status retrieved")
    public ResponseEntity<java.util.Map<String, Boolean>> getPresence(
            @RequestParam String userIds) {
        java.util.Map<String, Boolean> result = new java.util.LinkedHashMap<>();
        for (String uid : userIds.split(",")) {
            String trimmed = uid.trim();
            if (!trimmed.isEmpty()) {
                result.put(trimmed, presenceManager.isOnline(trimmed));
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count across all conversations")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(java.util.Map.of("unreadCount", conversationService.getTotalUnreadCount(userId)));
    }
}
