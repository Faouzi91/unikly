package com.unikly.jobservice.job.domain.model;

public class InvalidProposalStateException extends RuntimeException {

    public InvalidProposalStateException(String message) {
        super(message);
    }
}
