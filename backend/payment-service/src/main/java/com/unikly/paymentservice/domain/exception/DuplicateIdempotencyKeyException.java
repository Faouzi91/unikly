package com.unikly.paymentservice.domain.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException(String key) {
        super("Payment already exists for idempotency key: " + key);
    }
}
