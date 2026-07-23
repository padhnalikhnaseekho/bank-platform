package com.bankplatform.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String correlationId,
        List<FieldViolation> details) {

    public record FieldViolation(String field, String message) {}

    public static ApiError of(
            int status,
            String errorCode,
            String message,
            String correlationId,
            List<FieldViolation> details) {
        return new ApiError(Instant.now(), status, errorCode, message, correlationId, details);
    }
}
