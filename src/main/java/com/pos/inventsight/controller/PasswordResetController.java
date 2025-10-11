package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.PasswordResetConfirmRequest;
import com.pos.inventsight.dto.PasswordResetRequest;
import com.pos.inventsight.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for password reset flow with email delivery.
 * Public endpoints (no authentication required).
 */
@RestController
@RequestMapping("/auth/password-reset")
@Tag(name = "Password Reset", description = "Password Reset API")
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private MessageSource messageSource;
    
    @Value("${inventsight.password-reset.base-url:http://localhost:8080/api/auth/password-reset/reset}")
    private String resetBaseUrl;
    
    /**
     * Request password reset - sends email with reset link
     */
    @PostMapping("/request")
    @Operation(summary = "Request Password Reset", description = "Request a password reset link via email")
    public ResponseEntity<ApiResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.requestPasswordReset(request.getEmail(), resetBaseUrl);
            
            // Always return success to prevent email enumeration
            String message = messageSource.getMessage("password.reset.requested", null, LocaleContextHolder.getLocale());
            return ResponseEntity.ok(new ApiResponse(true, message));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error requesting password reset: " + e.getMessage()));
        }
    }
    
    /**
     * Validate password reset token
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate Reset Token", description = "Check if a password reset token is valid")
    public ResponseEntity<ApiResponse> validateToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validateToken(token);
            
            if (isValid) {
                return ResponseEntity.ok(new ApiResponse(true, "Token is valid"));
            } else {
                String message = messageSource.getMessage("password.reset.invalid", null, LocaleContextHolder.getLocale());
                return ResponseEntity.badRequest().body(new ApiResponse(false, message));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error validating token: " + e.getMessage()));
        }
    }
    
    /**
     * Confirm password reset with token and new password
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm Password Reset", description = "Reset password using the token from email")
    public ResponseEntity<ApiResponse> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            if (success) {
                String message = messageSource.getMessage("password.reset.success", null, LocaleContextHolder.getLocale());
                return ResponseEntity.ok(new ApiResponse(true, message));
            } else {
                String message = messageSource.getMessage("password.reset.invalid", null, LocaleContextHolder.getLocale());
                return ResponseEntity.badRequest().body(new ApiResponse(false, message));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error confirming password reset: " + e.getMessage()));
        }
    }
}
