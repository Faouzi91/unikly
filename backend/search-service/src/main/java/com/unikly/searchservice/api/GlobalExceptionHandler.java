package com.unikly.searchservice.api;

import com.unikly.common.error.ErrorResponse;
import com.unikly.common.error.GlobalExceptionHandlerBase;
import com.unikly.searchservice.api.exception.SearchUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(SearchUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSearchUnavailable(SearchUnavailableException ex) {
        log.error("Search service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "Service Unavailable",
                        "Search service temporarily unavailable. Please try again.", getTraceId()));
    }
}
