package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.MfaVerifyRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.MfaService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Multi-Factor Authentication (MFA) operations.
 * Provides endpoints for setup, verification, and backup codes.
 */
@RestController
@RequestMapping("/auth/mfa")
@Tag(name = "MFA", description = "Multi-Factor Authentication API")
public class MfaController {
    
    @Autowired
    private MfaService mfaService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private MessageSource messageSource;
    
    /**
     * Initialize MFA setup for authenticated user
     */
    @PostMapping("/setup")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Setup MFA", description = "Initialize MFA for the authenticated user - returns secret and QR code URL")
    public ResponseEntity<ApiResponse> setupMfa(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            MfaService.MfaSetupResponse response = mfaService.setupMfa(user);
            
            Map<String, Object> data = new HashMap<>();
            data.put("secret", response.getSecret());
            data.put("qrCodeUrl", response.getQrCodeUrl());
            
            String message = messageSource.getMessage("mfa.setup.success", null, LocaleContextHolder.getLocale());
            return ResponseEntity.ok(new ApiResponse(true, message));
            
        } catch (IllegalStateException e) {
            String message = messageSource.getMessage("mfa.already.enabled", null, LocaleContextHolder.getLocale());
            return ResponseEntity.badRequest().body(new ApiResponse(false, message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error setting up MFA: " + e.getMessage()));
        }
    }
    
    /**
     * Verify TOTP code and enable MFA
     */
    @PostMapping("/verify")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify MFA", description = "Verify TOTP code and enable MFA")
    public ResponseEntity<ApiResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                   Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            boolean isValid = mfaService.verifyAndEnable(user, request.getCode());
            
            if (isValid) {
                String message = messageSource.getMessage("mfa.verify.success", null, LocaleContextHolder.getLocale());
                return ResponseEntity.ok(new ApiResponse(true, message));
            } else {
                String message = messageSource.getMessage("mfa.verify.failed", null, LocaleContextHolder.getLocale());
                return ResponseEntity.badRequest().body(new ApiResponse(false, message));
            }
            
        } catch (IllegalStateException e) {
            String message = messageSource.getMessage("mfa.not.enabled", null, LocaleContextHolder.getLocale());
            return ResponseEntity.badRequest().body(new ApiResponse(false, message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error verifying MFA: " + e.getMessage()));
        }
    }
    
    /**
     * Generate backup codes for account recovery
     */
    @PostMapping("/backup-codes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate Backup Codes", description = "Generate one-time use backup codes for account recovery")
    public ResponseEntity<ApiResponse> generateBackupCodes(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<String> backupCodes = mfaService.generateBackupCodes(user);
            
            String codesString = String.join(", ", backupCodes);
            String message = messageSource.getMessage("mfa.backup.generated", null, LocaleContextHolder.getLocale()) 
                    + ". Codes: " + codesString 
                    + ". WARNING: Store these codes securely. They can only be used once and will not be shown again.";
            return ResponseEntity.ok(new ApiResponse(true, message));
            
        } catch (IllegalStateException e) {
            String message = messageSource.getMessage("mfa.not.enabled", null, LocaleContextHolder.getLocale());
            return ResponseEntity.badRequest().body(new ApiResponse(false, message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error generating backup codes: " + e.getMessage()));
        }
    }
    
    /**
     * Check MFA status for authenticated user
     */
    @GetMapping("/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Check MFA Status", description = "Check if MFA is enabled for the authenticated user")
    public ResponseEntity<ApiResponse> getMfaStatus(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            boolean enabled = mfaService.isMfaEnabled(user);
            
            String message = "MFA status: " + (enabled ? "enabled" : "disabled");
            return ResponseEntity.ok(new ApiResponse(true, message));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error retrieving MFA status: " + e.getMessage()));
        }
    }
    
    /**
     * Disable MFA for authenticated user
     */
    @DeleteMapping("/disable")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Disable MFA", description = "Disable MFA for the authenticated user")
    public ResponseEntity<ApiResponse> disableMfa(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            mfaService.disableMfa(user);
            
            String message = "MFA disabled successfully";
            return ResponseEntity.ok(new ApiResponse(true, message));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error disabling MFA: " + e.getMessage()));
        }
    }
}
