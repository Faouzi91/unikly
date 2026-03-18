package com.unikly.notificationservice.infrastructure;

import com.unikly.notificationservice.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
}
