package com.unikly.notificationservice.application.port.out;

import com.unikly.notificationservice.domain.model.JobClientCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobClientCacheRepository extends JpaRepository<JobClientCache, UUID> {
}
