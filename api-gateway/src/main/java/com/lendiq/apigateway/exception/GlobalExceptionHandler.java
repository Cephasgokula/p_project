package com.lendiq.apigateway.exception;

import com.lendiq.apigateway.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), traceId(), Instant.now());
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateApplicationException ex) {
        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), traceId(), Instant.now());
    }

    @ExceptionHandler(VelocityFraudException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleVelocity(VelocityFraudException ex) {
        return new ErrorResponse("VELOCITY_EXCEEDED", "Rate limit exceeded", traceId(), Instant.now());
    }

    @ExceptionHandler(InsufficientPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(InsufficientPermissionException ex) {
        return new ErrorResponse("FORBIDDEN", "Access denied", traceId(), Instant.now());
    }

    @ExceptionHandler(ScoringServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleScoringDown(ScoringServiceUnavailableException ex) {
        return new ErrorResponse("SCORING_UNAVAILABLE",
            "Scoring service temporarily unavailable — please retry", traceId(), Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return new ErrorResponse("VALIDATION_FAILED", msg, traceId(), Instant.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception [traceId={}]", traceId(), ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", traceId(), Instant.now());
    }

    private String traceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
