package com.unikly.paymentservice.domain.exception;

public class PaymentAccessDeniedException extends RuntimeException {
    public PaymentAccessDeniedException() {
        super("You are not the owner of this payment");
    }
}
