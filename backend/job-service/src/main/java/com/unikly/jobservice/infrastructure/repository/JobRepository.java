package com.unikly.jobservice.infrastructure.repository;

import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    long countByStatus(JobStatus status);

    Page<Job> findByClientId(UUID clientId, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @Query("SELECT j FROM Job j JOIN j.skills s WHERE j.status = :status AND s LIKE %:skill%")
    List<Job> findByStatusAndSkillsContaining(@Param("status") JobStatus status, @Param("skill") String skill);

    @Query("SELECT COUNT(p) FROM Proposal p WHERE p.jobId = :jobId AND p.status NOT IN ('REJECTED', 'WITHDRAWN')")
    long countActiveProposalsByJobId(@Param("jobId") UUID jobId);

    @Query("SELECT j FROM Job j WHERE j.id IN " +
           "(SELECT p.jobId FROM Proposal p WHERE p.freelancerId = :freelancerId AND p.status = 'ACCEPTED')")
    Page<Job> findByFreelancerId(@Param("freelancerId") UUID freelancerId, Pageable pageable);
}
