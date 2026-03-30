package com.unikly.jobservice.invitation.application.port.out;

import com.unikly.jobservice.invitation.domain.model.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Page<Invitation> findByJobId(UUID jobId, Pageable pageable);
    Page<Invitation> findByFreelancerId(UUID freelancerId, Pageable pageable);
    boolean existsByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);
    Optional<Invitation> findByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);
}
