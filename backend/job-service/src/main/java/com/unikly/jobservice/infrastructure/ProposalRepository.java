package com.unikly.jobservice.infrastructure;

import com.unikly.jobservice.domain.Proposal;
import com.unikly.jobservice.domain.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByJobId(UUID jobId, Pageable pageable);

    List<Proposal> findByJobIdAndStatus(UUID jobId, ProposalStatus status);

    long countByJobId(UUID jobId);

    @Modifying
    @Query("UPDATE Proposal p SET p.status = :status WHERE p.jobId = :jobId AND p.status = 'PENDING' AND p.id <> :excludeId")
    void rejectOtherPendingProposals(UUID jobId, UUID excludeId, ProposalStatus status);
}
