package com.unikly.searchservice.api.exception;

public class SearchUnavailableException extends RuntimeException {
    public SearchUnavailableException(String message) {
        super(message);
    }
}
