package com.unikly.jobservice.api;

import com.unikly.common.error.ErrorResponse;
import com.unikly.common.error.GlobalExceptionHandlerBase;
import com.unikly.jobservice.domain.InvalidStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleStatusTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden",
                        "You don't have permission for this action", getTraceId()));
    }
}
