// src/main/java/com/ecommerce/product/exception/GlobalExceptionHandler.java
package com.ecommerce.product.exception;

import com.ecommerce.product.common.CorrelationIdHolder;
import com.ecommerce.product.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(
                        fe.getField(), fe.getDefaultMessage(),
                        sanitize(fe.getField(), fe.getRejectedValue())))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                java.time.Instant.now(), 400, "Bad Request",
                "Validation failed on request body",
                request.getRequestURI(), CorrelationIdHolder.getCorrelationId(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(
                        v.getPropertyPath().toString(), v.getMessage(), v.getInvalidValue()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                java.time.Instant.now(), 400, "Bad Request",
                "Validation failed on request parameters",
                request.getRequestURI(), CorrelationIdHolder.getCorrelationId(), errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request",
                "Malformed or unreadable JSON request body",
                request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request",
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
                request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(404, "Not Found",
                ex.getMessage(), request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSku(
            DuplicateSkuException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, "Conflict",
                ex.getMessage(), request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    /**
     * JPA optimistic locking failure — two concurrent writes to the same product.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock failure on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, "Conflict",
                "The product was modified by another request. Please retry.",
                request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    /**
     * MySQL unique constraint violation — e.g. duplicate SKU on INSERT.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("uk_products_sku")) {
            message = "A product with this SKU already exists";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, "Conflict",
                message, request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(422, "Unprocessable Entity",
                ex.getMessage(), request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.of(500, "Internal Server Error",
                "An unexpected error occurred", request.getRequestURI(), CorrelationIdHolder.getCorrelationId()));
    }

    private Object sanitize(String field, Object value) {
        return value;
    }
}