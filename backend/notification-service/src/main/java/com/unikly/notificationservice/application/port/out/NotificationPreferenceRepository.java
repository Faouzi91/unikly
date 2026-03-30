package com.unikly.notificationservice.application.port.out;

import com.unikly.notificationservice.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
}
