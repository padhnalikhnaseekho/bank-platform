package com.bankplatform.common.error;

import com.bankplatform.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an {@link ApiError} body directly to the response for failures that occur inside
 * the Spring Security filter chain (authentication/authorization), which never reach
 * {@code @RestControllerAdvice}.
 */
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String errorCode, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        ApiError body = ApiError.of(status.value(), errorCode, message, correlationId, List.of());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
