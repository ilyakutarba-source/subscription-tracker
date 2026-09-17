package com.example.subscriptiontracker.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubscriptionNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(SubscriptionNotFoundException exception,
                                            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
                                              HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid", request, fields);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage(), request, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String error, String message,
                                           HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), error, message, request.getRequestURI(), fields));
    }
}

