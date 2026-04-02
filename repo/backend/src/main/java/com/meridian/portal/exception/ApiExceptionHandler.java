package com.meridian.portal.exception;

import com.meridian.portal.health.service.HealthMonitoringService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(basePackages = "com.meridian.portal", annotations = RestController.class)
public class ApiExceptionHandler {
    private static final String MALFORMED_JSON_REASON_CODE = "MALFORMED_JSON";

    private final HealthMonitoringService healthMonitoringService;

    public ApiExceptionHandler(HealthMonitoringService healthMonitoringService) {
        this.healthMonitoringService = healthMonitoringService;
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        logError(404, request, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiErrorResponse(Instant.now(), 404, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        logError(409, request, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiErrorResponse(Instant.now(), 409, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        logError(400, request, ex.getMessage(), null);
        return ResponseEntity.badRequest()
            .body(new ApiErrorResponse(Instant.now(), 400, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldMessage)
            .collect(Collectors.toList());
        logError(400, request, "Validation failed", details.toString());

        return ResponseEntity.badRequest()
            .body(new ApiErrorResponse(Instant.now(), 400, "Validation failed", request.getRequestURI(), details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logError(400, request, "Malformed request body", MALFORMED_JSON_REASON_CODE);
        return ResponseEntity.badRequest()
            .body(new ApiErrorResponse(Instant.now(), 400, "Malformed request body", request.getRequestURI(), List.of()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        logError(403, request, "Forbidden", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiErrorResponse(Instant.now(), 403, "Forbidden", request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        logError(500, request, "Internal server error", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiErrorResponse(Instant.now(), 500, "Internal server error", request.getRequestURI(), List.of()));
    }

    private String toFieldMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private void logError(int status, HttpServletRequest request, String message, String details) {
        healthMonitoringService.logError(status, request.getRequestURI(), message, details, Instant.now());
    }

    public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path,
        List<String> details
    ) {}
}
