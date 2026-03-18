package com.unikly.paymentservice.api.dto;

import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentId,
        String clientSecret
) {}
