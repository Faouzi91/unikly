package com.unikly.common.dto;

import java.math.BigDecimal;

public record Money(
        BigDecimal amount,
        String currency
) {
}
