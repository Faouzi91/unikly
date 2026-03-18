package com.unikly.jobservice.infrastructure;

import com.unikly.jobservice.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByJobId(UUID jobId);
}
