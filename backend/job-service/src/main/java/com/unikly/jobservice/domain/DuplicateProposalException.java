package com.unikly.jobservice.domain;

public class DuplicateProposalException extends RuntimeException {

    public DuplicateProposalException(String message) {
        super(message);
    }
}
