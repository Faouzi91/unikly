package com.unikly.notificationservice.adapter.in.web.dto;

import java.time.LocalTime;

public record NotificationPreferenceRequest(
        boolean emailEnabled,
        boolean pushEnabled,
        boolean realtimeEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}
