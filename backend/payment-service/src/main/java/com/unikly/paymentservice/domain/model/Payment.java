
package com.unikly.paymentservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "freelancer_id", nullable = false)
    private UUID freelancerId;

    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    @Setter
    @Column(name = "stripe_transfer_id", length = 255)
    private String stripeTransferId;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    public static Payment create(UUID jobId, UUID clientId, UUID freelancerId,
                                  BigDecimal amount, String currency,
                                  String idempotencyKey, String stripePaymentIntentId) {
        Payment p = new Payment();
        p.jobId = jobId;
        p.clientId = clientId;
        p.freelancerId = freelancerId;
        p.amount = amount;
        p.currency = currency;
        p.idempotencyKey = idempotencyKey;
        p.stripePaymentIntentId = stripePaymentIntentId;
        p.status = PaymentStatus.PENDING;
        return p;
    }

    public void transitionTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Invalid payment transition: %s → %s".formatted(status, next));
        }
        this.status = next;
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}
