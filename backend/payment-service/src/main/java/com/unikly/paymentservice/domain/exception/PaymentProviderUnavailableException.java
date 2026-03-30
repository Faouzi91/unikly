package com.unikly.paymentservice.domain.exception;

public class PaymentProviderUnavailableException extends RuntimeException {
    public PaymentProviderUnavailableException(String message) {
        super(message);
    }
}
