package com.unikly.matchingservice.application.port.out;

import com.unikly.matchingservice.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import com.unikly.matchingservice.domain.model.*;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
