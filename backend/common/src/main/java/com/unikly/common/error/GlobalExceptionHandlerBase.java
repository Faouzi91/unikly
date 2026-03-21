package com.unikly.common.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.UUID;

/**
 * Base class for all service GlobalExceptionHandlers.
 * Provides consistent error response format across all services.
 * Subclasses should add {@code @RestControllerAdvice} and service-specific handlers.
 */
@Slf4j
public abstract class GlobalExceptionHandlerBase {

    protected String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null && !traceId.isBlank() ? traceId : UUID.randomUUID().toString();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorDetail(e.getField(),
                        e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid"))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                400, "Validation Failed",
                "One or more fields have invalid values",
                getTraceId(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                400, "Validation Failed",
                "One or more constraints were violated",
                getTraceId(), details));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden",
                        "You don't have permission for this action", getTraceId()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict",
                        "Resource modified by another request", getTraceId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String traceId = getTraceId();
        log.error("Unhandled exception [traceId={}]", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "Something went wrong. Please try again later.", traceId));
    }
}
