package com.unikly.store.orders.domain;

public enum OrderFulfillmentStatus {
    PLACED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELED;

    public OrderFulfillmentStatus next() {
        return switch (this) {
            case PLACED -> PROCESSING;
            case PROCESSING -> SHIPPED;
            case SHIPPED -> DELIVERED;
            case DELIVERED, CANCELED -> null;
        };
    }
}
