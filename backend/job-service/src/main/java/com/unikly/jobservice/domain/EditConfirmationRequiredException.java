package com.unikly.jobservice.domain;

public class EditConfirmationRequiredException extends RuntimeException {

    private final EditDecision decision;

    public EditConfirmationRequiredException(EditDecision decision) {
        super(decision.message());
        this.decision = decision;
    }

    public EditDecision getDecision() {
        return decision;
    }
}
