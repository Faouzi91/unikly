package com.unikly.notificationservice.infrastructure;

import com.unikly.notificationservice.domain.JobClientCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobClientCacheRepository extends JpaRepository<JobClientCache, UUID> {
}
