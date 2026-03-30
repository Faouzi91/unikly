package com.unikly.jobservice.common.adapter.in.web;

import com.unikly.common.error.ErrorResponse;
import com.unikly.common.error.GlobalExceptionHandlerBase;
import com.unikly.jobservice.proposal.domain.exception.DuplicateProposalException;
import com.unikly.jobservice.job.domain.model.EditConfirmationRequiredException;
import com.unikly.jobservice.job.domain.model.InvalidProposalStateException;
import com.unikly.jobservice.job.domain.model.InvalidStateTransitionException;
import com.unikly.jobservice.job.domain.model.JobNotEditableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleStatusTransition(InvalidStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden",
                        "You don't have permission for this action", getTraceId()));
    }

    @ExceptionHandler(EditConfirmationRequiredException.class)
    public ResponseEntity<?> handleEditConfirmationRequired(EditConfirmationRequiredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getDecision());
    }

    @ExceptionHandler(JobNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleJobNotEditable(JobNotEditableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(DuplicateProposalException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProposal(DuplicateProposalException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(InvalidProposalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProposalState(InvalidProposalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), getTraceId()));
    }
}
