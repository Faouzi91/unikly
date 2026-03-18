package com.unikly.matchingservice.infrastructure;

import com.unikly.matchingservice.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
