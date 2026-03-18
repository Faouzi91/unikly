package com.unikly.paymentservice.infrastructure;

import com.unikly.paymentservice.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByPaymentId(UUID paymentId);
}
