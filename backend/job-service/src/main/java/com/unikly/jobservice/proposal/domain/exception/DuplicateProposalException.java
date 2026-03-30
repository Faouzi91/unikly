package com.unikly.jobservice.proposal.domain.exception;

public class DuplicateProposalException extends RuntimeException {

    public DuplicateProposalException(String message) {
        super(message);
    }
}
