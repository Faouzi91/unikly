package com.unikly.jobservice.infrastructure;

import com.unikly.jobservice.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    long countByStatus(com.unikly.jobservice.domain.JobStatus status);
}
