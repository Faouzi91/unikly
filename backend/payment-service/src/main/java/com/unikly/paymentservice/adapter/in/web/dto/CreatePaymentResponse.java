package com.unikly.paymentservice.adapter.in.web.dto;

import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentId,
        String clientSecret
) {}
