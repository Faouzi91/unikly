package com.unikly.jobservice.domain;

public class JobNotEditableException extends RuntimeException {
    public JobNotEditableException(String message) {
        super(message);
    }
}
