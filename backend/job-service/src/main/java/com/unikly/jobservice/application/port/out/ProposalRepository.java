package com.unikly.jobservice.application.port.out;

import com.unikly.jobservice.domain.model.Proposal;
import com.unikly.jobservice.domain.model.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    Page<Proposal> findByJobId(UUID jobId, Pageable pageable);

    Page<Proposal> findByFreelancerId(UUID freelancerId, Pageable pageable);

    List<Proposal> findByJobIdAndStatusIn(UUID jobId, List<ProposalStatus> statuses);

    List<Proposal> findByJobIdAndStatus(UUID jobId, ProposalStatus status);

    boolean existsByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);

    Optional<Proposal> findByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);

    long countByJobIdAndStatusNotIn(UUID jobId, List<ProposalStatus> statuses);

    @Modifying
    @Query("UPDATE Proposal p SET p.status = :status WHERE p.jobId = :jobId AND p.status = 'PENDING' AND p.id <> :excludeId")
    void rejectOtherPendingProposals(@Param("jobId") UUID jobId, @Param("excludeId") UUID excludeId, @Param("status") ProposalStatus status);

    @Query("SELECT p.freelancerId FROM Proposal p WHERE p.jobId = :jobId AND p.status IN :statuses")
    List<UUID> findFreelancerIdsByJobIdAndStatusIn(@Param("jobId") UUID jobId,
                                                   @Param("statuses") List<ProposalStatus> statuses);

    @Modifying
    @Query("UPDATE Proposal p SET p.status = :newStatus WHERE p.jobId = :jobId AND p.status IN :currentStatuses")
    int bulkUpdateStatusByJobId(@Param("jobId") UUID jobId,
                                @Param("currentStatuses") List<ProposalStatus> currentStatuses,
                                @Param("newStatus") ProposalStatus newStatus);
}
