package com.unikly.paymentservice.application.exception;

public class PaymentAccessDeniedException extends RuntimeException {
    public PaymentAccessDeniedException() {
        super("You are not the owner of this payment");
    }
}
