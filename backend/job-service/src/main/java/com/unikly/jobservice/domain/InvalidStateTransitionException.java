package com.unikly.jobservice.domain;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(JobStatus current, JobStatus target) {
        super("Invalid state transition: %s → %s".formatted(current, target));
    }
}
