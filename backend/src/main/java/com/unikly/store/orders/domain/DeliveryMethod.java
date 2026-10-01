package com.unikly.store.orders.domain;

import java.math.BigDecimal;

public enum DeliveryMethod {
    STANDARD("Standard Delivery", "3-5 business days", new BigDecimal("5.00"), new BigDecimal("50.00")),
    EXPRESS("Express Delivery", "1-2 business days", new BigDecimal("15.00"), null);

    private final String displayName;
    private final String estimatedDelivery;
    private final BigDecimal baseFee;
    private final BigDecimal freeThreshold;

    DeliveryMethod(String displayName, String estimatedDelivery, BigDecimal baseFee, BigDecimal freeThreshold) {
        this.displayName = displayName;
        this.estimatedDelivery = estimatedDelivery;
        this.baseFee = baseFee;
        this.freeThreshold = freeThreshold;
    }

    public BigDecimal calculateFee(BigDecimal subtotal) {
        if (this == STANDARD && freeThreshold != null && subtotal.compareTo(freeThreshold) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return baseFee.setScale(2);
    }

    public String displayName() {
        return displayName;
    }

    public String estimatedDelivery() {
        return estimatedDelivery;
    }

    public BigDecimal baseFee() {
        return baseFee;
    }

    public BigDecimal freeThreshold() {
        return freeThreshold;
    }
}
