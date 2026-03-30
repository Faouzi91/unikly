package com.unikly.paymentservice.adapter.in.web;

import com.unikly.common.error.ErrorResponse;
import com.unikly.common.error.GlobalExceptionHandlerBase;
import com.unikly.paymentservice.domain.exception.DuplicateIdempotencyKeyException;
import com.unikly.paymentservice.domain.exception.InvalidPaymentStateException;
import com.unikly.paymentservice.domain.exception.PaymentAccessDeniedException;
import com.unikly.paymentservice.domain.exception.PaymentNotFoundException;
import com.unikly.paymentservice.domain.exception.PaymentProviderUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateIdempotencyKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidPaymentStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(PaymentAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden",
                        "You don't have permission for this action", getTraceId()));
    }

    @ExceptionHandler(PaymentProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProviderUnavailable(PaymentProviderUnavailableException ex) {
        log.error("Payment provider unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "Service Unavailable",
                        "Payment service temporarily unavailable. Please try again.", getTraceId()));
    }
}
