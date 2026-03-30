package com.unikly.notificationservice.adapter.in.web.dto;

import com.unikly.notificationservice.domain.model.Notification;
import com.unikly.notificationservice.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String body,
        boolean read,
        String actionUrl,
        Instant createdAt
) {
    public static NotificationResponse from(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.isRead(),
                entity.getActionUrl(),
                entity.getCreatedAt()
        );
    }
}
