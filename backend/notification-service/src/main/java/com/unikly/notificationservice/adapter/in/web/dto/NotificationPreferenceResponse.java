package com.unikly.notificationservice.adapter.in.web.dto;

import com.unikly.notificationservice.domain.model.NotificationPreference;

import java.time.LocalTime;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID userId,
        boolean emailEnabled,
        boolean pushEnabled,
        boolean realtimeEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
    public static NotificationPreferenceResponse from(NotificationPreference entity) {
        return new NotificationPreferenceResponse(
                entity.getUserId(),
                entity.isEmailEnabled(),
                entity.isPushEnabled(),
                entity.isRealtimeEnabled(),
                entity.getQuietHoursStart(),
                entity.getQuietHoursEnd()
        );
    }
}
