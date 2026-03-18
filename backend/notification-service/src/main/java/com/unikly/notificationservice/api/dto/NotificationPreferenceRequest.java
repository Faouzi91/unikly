package com.unikly.notificationservice.api.dto;

import java.time.LocalTime;

public record NotificationPreferenceRequest(
        boolean emailEnabled,
        boolean pushEnabled,
        boolean realtimeEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}
