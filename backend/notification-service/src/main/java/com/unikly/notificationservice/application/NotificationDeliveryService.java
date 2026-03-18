package com.unikly.notificationservice.application;

import com.unikly.notificationservice.api.dto.NotificationResponse;
import com.unikly.notificationservice.domain.Notification;
import com.unikly.notificationservice.domain.NotificationPreference;
import com.unikly.notificationservice.domain.NotificationType;
import com.unikly.notificationservice.infrastructure.NotificationPreferenceRepository;
import com.unikly.notificationservice.infrastructure.NotificationRepository;
import com.unikly.notificationservice.infrastructure.WebSocketPresenceManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final WebSocketPresenceManager presenceManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createAndDeliver(UUID userId,
                                         NotificationType type,
                                         String title,
                                         String body,
                                         String actionUrl) {
        var notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .read(false)
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);

        var prefs = preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.defaultFor(userId));

        if (prefs.isRealtimeEnabled() && presenceManager.isOnline(userId.toString())) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    NotificationResponse.from(notification));
            log.info("WebSocket notification delivered: userId={}, type={}", userId, type);
        } else if (prefs.isPushEnabled()) {
            log.info("Would send FCM push to userId={}, type={}", userId, type);
        }

        return notification;
    }

    public Page<Notification> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public Notification markRead(UUID notificationId, UUID userId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to this user");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }

    public NotificationPreference getPreferences(UUID userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.defaultFor(userId));
    }

    @Transactional
    public NotificationPreference savePreferences(NotificationPreference prefs) {
        return preferenceRepository.save(prefs);
    }
}
