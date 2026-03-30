package com.unikly.jobservice.adapter.out.persistence;

import com.unikly.jobservice.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
