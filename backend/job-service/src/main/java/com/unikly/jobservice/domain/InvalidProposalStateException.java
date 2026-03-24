package com.unikly.jobservice.domain;

public class InvalidProposalStateException extends RuntimeException {

    public InvalidProposalStateException(String message) {
        super(message);
    }
}
