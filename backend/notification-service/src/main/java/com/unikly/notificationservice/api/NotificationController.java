package com.unikly.notificationservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.notificationservice.api.dto.NotificationPreferenceRequest;
import com.unikly.notificationservice.api.dto.NotificationPreferenceResponse;
import com.unikly.notificationservice.api.dto.NotificationResponse;
import com.unikly.notificationservice.application.NotificationDeliveryService;
import com.unikly.notificationservice.domain.NotificationPreference;
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
public class NotificationController {

    private final NotificationDeliveryService deliveryService;

    /** GET /api/v1/notifications?unread=true&page=0&size=20 */
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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

    /** PATCH /api/v1/notifications/{id}/read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(NotificationResponse.from(deliveryService.markRead(id, userId)));
    }

    /** PATCH /api/v1/notifications/read-all */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead() {
        UUID userId = UserContext.getUserId();
        int count = deliveryService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("markedRead", count));
    }

    /** GET /api/v1/notifications/preferences */
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(NotificationPreferenceResponse.from(deliveryService.getPreferences(userId)));
    }

    /** PUT /api/v1/notifications/preferences */
    @PutMapping("/preferences")
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
