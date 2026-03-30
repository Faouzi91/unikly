package com.unikly.jobservice.job.domain.model;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(JobStatus current, JobStatus target) {
        super("Invalid state transition: %s → %s".formatted(current, target));
    }
}
