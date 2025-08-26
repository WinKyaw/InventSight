package com.pos.inventsight.exception;

import com.pos.inventsight.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        System.out.println("🔍 InventSight - Resource not found: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false),
            "RESOURCE_NOT_FOUND",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        System.out.println("🔄 InventSight - Duplicate resource: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false),
            "DUPLICATE_RESOURCE",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<?> handleInsufficientStockException(InsufficientStockException ex, WebRequest request) {
        System.out.println("📦 InventSight - Insufficient stock: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false),
            "INSUFFICIENT_STOCK",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        System.out.println("🔐 InventSight - Authentication failed: Invalid credentials");
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            "Invalid email or password",
            request.getDescription(false),
            "INVALID_CREDENTIALS",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        System.out.println("🚫 InventSight - Access denied: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            "Access denied - Insufficient privileges for InventSight system",
            request.getDescription(false),
            "ACCESS_DENIED",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        System.out.println("✏️ InventSight - Validation error: Invalid request data");
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "InventSight validation failed",
            errors,
            LocalDateTime.now(),
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        System.out.println("🗄️ InventSight - Database constraint violation: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        
        String message = "InventSight database constraint violation - please check your data";
        if (ex.getMessage().contains("unique constraint")) {
            message = "InventSight duplicate entry - this record already exists";
        }
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            message,
            request.getDescription(false),
            "DATABASE_CONSTRAINT_VIOLATION",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        System.out.println("💥 InventSight - Unexpected error: " + ex.getMessage());
        System.out.println("📅 Error time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        System.out.println("🔍 Stack trace: ");
        ex.printStackTrace();
        
        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            "An unexpected error occurred in InventSight system. Please try again later.",
            request.getDescription(false),
            "INTERNAL_SERVER_ERROR",
            "InventSight System"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }
    
    // Error response classes
    public static class ErrorDetails {
        private LocalDateTime timestamp;
        private String message;
        private String details;
        private String errorCode;
        private String system;
        
        public ErrorDetails(LocalDateTime timestamp, String message, String details, String errorCode, String system) {
            this.timestamp = timestamp;
            this.message = message;
            this.details = details;
            this.errorCode = errorCode;
            this.system = system;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        public String getErrorCode() { return errorCode; }
        public String getSystem() { return system; }
    }
    
    public static class ValidationErrorResponse {
        private String message;
        private Map<String, String> fieldErrors;
        private LocalDateTime timestamp;
        private String system;
        
        public ValidationErrorResponse(String message, Map<String, String> fieldErrors, LocalDateTime timestamp, String system) {
            this.message = message;
            this.fieldErrors = fieldErrors;
            this.timestamp = timestamp;
            this.system = system;
        }
        
        // Getters
        public String getMessage() { return message; }
        public Map<String, String> getFieldErrors() { return fieldErrors; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSystem() { return system; }
    }
}