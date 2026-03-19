package com.unikly.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String traceId,
        String timestamp,
        List<ErrorDetail> details
) {
    public static ErrorResponse of(int status, String error, String message, String traceId) {
        return new ErrorResponse(status, error, message, traceId, Instant.now().toString(), List.of());
    }

    public static ErrorResponse of(int status, String error, String message, String traceId, List<ErrorDetail> details) {
        return new ErrorResponse(status, error, message, traceId, Instant.now().toString(), details);
    }
}
