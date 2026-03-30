package com.unikly.paymentservice.domain.model;

import java.util.Set;

public enum PaymentStatus {
    PENDING,
    FUNDED,
    RELEASED,
    COMPLETED,
    FAILED,
    REFUNDED,
    DISPUTED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING   -> Set.of(FUNDED, FAILED).contains(next);
            case FUNDED    -> Set.of(RELEASED, REFUNDED, DISPUTED).contains(next);
            case RELEASED  -> Set.of(COMPLETED).contains(next);
            case DISPUTED  -> Set.of(REFUNDED, RELEASED).contains(next);
            default        -> false;
        };
    }
}
