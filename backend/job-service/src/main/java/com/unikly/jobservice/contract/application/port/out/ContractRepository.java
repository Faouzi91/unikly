package com.unikly.jobservice.contract.application.port.out;

import com.unikly.jobservice.contract.domain.model.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByJobId(UUID jobId);

    @Query("SELECT c FROM Contract c WHERE c.clientId = :userId OR c.freelancerId = :userId")
    Page<Contract> findByParticipant(@Param("userId") UUID userId, Pageable pageable);
}
