// src/main/java/com/ecommerce/user/exception/GlobalExceptionHandler.java
package com.ecommerce.user.exception;

import com.ecommerce.user.common.CorrelationIdHolder;
import com.ecommerce.user.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400: Validation failures on @RequestBody fields ─────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ErrorResponse error = new ErrorResponse(
                java.time.Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed on request body",
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId(),
                fieldErrors
        );

        log.debug("Validation error on {}: {} field errors", request.getRequestURI(), fieldErrors.size());
        return ResponseEntity.badRequest().body(error);
    }

    // ── 400: Validation failures on @RequestParam / @PathVariable ────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ErrorResponse error = new ErrorResponse(
                java.time.Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed on request parameters",
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(error);
    }

    // ── 400: Malformed JSON ──────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.debug("Malformed JSON on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Malformed or unreadable JSON request body",
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // ── 400: Type mismatch (e.g. invalid UUID in path) ───────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // ── 404: User not found ──────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {

        log.debug("User not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ── 409: Conflict (duplicate email) ──────────────────────────────

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex, HttpServletRequest request) {

        log.debug("Email conflict: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ── 422: Business rule violations ────────────────────────────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.unprocessableEntity().body(error);
    }

    // ── 500: Everything else ─────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                CorrelationIdHolder.getCorrelationId()
        );

        return ResponseEntity.internalServerError().body(error);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fe) {
        return new ErrorResponse.FieldErrorDetail(
                fe.getField(),
                fe.getDefaultMessage(),
                sanitizeRejectedValue(fe.getField(), fe.getRejectedValue())
        );
    }

    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(ConstraintViolation<?> v) {
        String field = v.getPropertyPath().toString();
        return new ErrorResponse.FieldErrorDetail(
                field,
                v.getMessage(),
                sanitizeRejectedValue(field, v.getInvalidValue())
        );
    }

    private Object sanitizeRejectedValue(String fieldName, Object rejectedValue) {
        if (fieldName != null && fieldName.toLowerCase().contains("password")) {
            return "***";
        }
        return rejectedValue;
    }
}