package com.shopcore.user.controller;

import com.shopcore.user.config.CorrelationIdFilter;
import com.shopcore.user.controller.dto.ApiErrorResponse;
import com.shopcore.user.service.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    ResponseEntity<ApiErrorResponse> handleServiceException(
            ServiceException exception,
            HttpServletRequest request
    ) {
        return response(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "Username or email already exists", request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "USER_CONCURRENT_MODIFICATION", "User was modified concurrently", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String correlationId = value == null ? "unknown" : value.toString();
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                code,
                message,
                status.value(),
                request.getRequestURI(),
                correlationId,
                Instant.now()
        ));
    }
}
