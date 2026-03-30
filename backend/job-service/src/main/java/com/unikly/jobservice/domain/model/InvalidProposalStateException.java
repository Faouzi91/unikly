package com.unikly.jobservice.domain.model;

public class InvalidProposalStateException extends RuntimeException {

    public InvalidProposalStateException(String message) {
        super(message);
    }
}
