package com.unikly.jobservice.domain.model;

public class JobNotEditableException extends RuntimeException {
    public JobNotEditableException(String message) {
        super(message);
    }
}
