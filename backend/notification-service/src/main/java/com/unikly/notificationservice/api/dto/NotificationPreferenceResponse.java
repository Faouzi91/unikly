package com.unikly.notificationservice.api.dto;

import com.unikly.notificationservice.domain.NotificationPreference;

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
