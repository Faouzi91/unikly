package com.unikly.jobservice.domain.model;

public class DuplicateProposalException extends RuntimeException {

    public DuplicateProposalException(String message) {
        super(message);
    }
}
