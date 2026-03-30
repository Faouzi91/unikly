package com.unikly.paymentservice.infrastructure;

import com.unikly.paymentservice.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findByJobIdAndStatus(UUID jobId, com.unikly.paymentservice.domain.PaymentStatus status);
    List<Payment> findByJobId(UUID jobId);
    List<Payment> findByClientIdOrFreelancerId(UUID clientId, UUID freelancerId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status IN :statuses")
    java.math.BigDecimal sumVolumeByStatuses(@org.springframework.data.repository.query.Param("statuses") List<com.unikly.paymentservice.domain.PaymentStatus> statuses);
}
