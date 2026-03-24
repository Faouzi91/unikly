package com.unikly.jobservice.domain;

import java.util.List;

public class EditConfirmationRequiredException extends RuntimeException {
    
    private final List<String> sensitiveFieldsChanged;

    public EditConfirmationRequiredException(String message, List<String> sensitiveFieldsChanged) {
        super(message);
        this.sensitiveFieldsChanged = sensitiveFieldsChanged;
    }

    public List<String> getSensitiveFieldsChanged() {
        return sensitiveFieldsChanged;
    }
}
