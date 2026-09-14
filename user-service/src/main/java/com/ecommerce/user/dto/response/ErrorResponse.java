// src/main/java/com/ecommerce/user/dto/response/ErrorResponse.java
package com.ecommerce.user.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String correlationId,
    List<FieldErrorDetail> validationErrors
) {
    public ErrorResponse {
        validationErrors = validationErrors == null ? List.of() : validationErrors;
    }

    /**
     * Factory for errors without validation field details.
     */
    public static ErrorResponse of(int status, String error, String message,
                                   String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message,
                                 path, correlationId, List.of());
    }

    public record FieldErrorDetail(String field, String message, Object rejectedValue) {}
}