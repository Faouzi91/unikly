package com.unikly.paymentservice.application.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException(String key) {
        super("Payment already exists for idempotency key: " + key);
    }
}
