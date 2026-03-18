package com.unikly.paymentservice.api.dto;

import com.unikly.paymentservice.domain.Payment;
import com.unikly.paymentservice.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID jobId,
        UUID clientId,
        UUID freelancerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String stripePaymentIntentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getJobId(), p.getClientId(), p.getFreelancerId(),
                p.getAmount(), p.getCurrency(), p.getStatus(),
                p.getStripePaymentIntentId(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
