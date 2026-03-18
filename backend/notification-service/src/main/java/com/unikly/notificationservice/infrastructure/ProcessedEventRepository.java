package com.unikly.notificationservice.infrastructure;

import com.unikly.notificationservice.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
