package com.unikly.jobservice.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(JobStatus current, JobStatus target) {
        super("Invalid status transition: %s → %s".formatted(current, target));
    }
}
