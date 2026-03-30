package com.unikly.jobservice.job.application.port.out;

import com.unikly.jobservice.job.domain.model.Job;
import com.unikly.jobservice.job.domain.model.JobStatus;
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

    @Query(value = "SELECT * FROM jobs WHERE status::text = :status AND :skill = ANY(skills)", nativeQuery = true)
    List<Job> findByStatusAndSkillsContaining(@Param("status") String status, @Param("skill") String skill);

    @Query("SELECT COUNT(p) FROM Proposal p WHERE p.jobId = :jobId AND p.status NOT IN ('REJECTED', 'WITHDRAWN')")
    long countActiveProposalsByJobId(@Param("jobId") UUID jobId);

    @Query("SELECT j FROM Job j WHERE j.id IN " +
           "(SELECT p.jobId FROM Proposal p WHERE p.freelancerId = :freelancerId AND p.status = 'ACCEPTED')")
    Page<Job> findByFreelancerId(@Param("freelancerId") UUID freelancerId, Pageable pageable);
}
