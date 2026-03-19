package com.unikly.paymentservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request to create an escrow payment intent")
public record CreatePaymentRequest(
        @Schema(description = "Job UUID this payment is for") @NotNull UUID jobId,
        @Schema(description = "Freelancer UUID receiving the payment") @NotNull UUID freelancerId,
        @Schema(description = "Payment amount", example = "1500.00") @Positive @NotNull BigDecimal amount,
        @Schema(description = "ISO 4217 currency code", example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,
        @Schema(description = "Unique idempotency key to prevent duplicate payments", example = "job-123-client-456-1700000000")
        @NotBlank @Size(max = 255) String idempotencyKey
) {}
