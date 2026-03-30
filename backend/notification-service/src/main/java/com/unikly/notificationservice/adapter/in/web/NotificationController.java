package com.unikly.notificationservice.adapter.in.web;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.notificationservice.adapter.in.web.dto.NotificationPreferenceRequest;
import com.unikly.notificationservice.adapter.in.web.dto.NotificationPreferenceResponse;
import com.unikly.notificationservice.adapter.in.web.dto.NotificationResponse;
import com.unikly.notificationservice.application.service.NotificationDeliveryService;
import com.unikly.notificationservice.domain.model.NotificationPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification delivery and preferences")
public class NotificationController {

    private final NotificationDeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns paginated notifications for the current user, optionally filtered to unread only")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @Parameter(description = "Return only unread notifications") @RequestParam(defaultValue = "false") boolean unread,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UUID userId = UserContext.getUserId();
        var notifications = deliveryService.getNotifications(userId, unread, PageRequest.of(page, size));
        var response = new PageResponse<>(
                notifications.getContent().stream().map(NotificationResponse::from).toList(),
                page,
                size,
                notifications.getTotalElements(),
                notifications.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationResponse> markRead(
            @Parameter(description = "Notification UUID") @PathVariable UUID id) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(NotificationResponse.from(deliveryService.markRead(id, userId)));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    public ResponseEntity<Map<String, Object>> markAllRead() {
        UUID userId = UserContext.getUserId();
        int count = deliveryService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("markedRead", count));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences")
    @ApiResponse(responseCode = "200", description = "Preferences retrieved")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(NotificationPreferenceResponse.from(deliveryService.getPreferences(userId)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    @ApiResponse(responseCode = "200", description = "Preferences updated")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @RequestBody NotificationPreferenceRequest request) {

        UUID userId = UserContext.getUserId();
        var prefs = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(request.emailEnabled())
                .pushEnabled(request.pushEnabled())
                .realtimeEnabled(request.realtimeEnabled())
                .quietHoursStart(request.quietHoursStart())
                .quietHoursEnd(request.quietHoursEnd())
                .build();
        return ResponseEntity.ok(NotificationPreferenceResponse.from(deliveryService.savePreferences(prefs)));
    }
}
