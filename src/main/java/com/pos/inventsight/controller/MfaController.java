package com.pos.inventsight.controller;

import com.pos.inventsight.dto.MfaVerifyRequest;
import com.pos.inventsight.dto.MfaSetupResponse;
import com.pos.inventsight.dto.MfaBackupCodesResponse;
import com.pos.inventsight.dto.MfaStatusResponse;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.MfaService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    
    /**
     * Initialize MFA setup for authenticated user
     */
    @PostMapping("/setup")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Setup MFA", description = "Initialize MFA for the authenticated user - returns secret and QR code URL")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            MfaService.MfaSetupResponse serviceResponse = mfaService.setupMfa(user);
            
            MfaSetupResponse.MfaSetupData data = new MfaSetupResponse.MfaSetupData(
                serviceResponse.getSecret(),
                serviceResponse.getQrCodeUrl(),
                serviceResponse.getQrCodeImage()
            );
            
            MfaSetupResponse response = new MfaSetupResponse(
                true,
                "MFA setup initiated successfully. Scan the QR code with your authenticator app.",
                data
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new MfaSetupResponse(false, "MFA is already enabled for this user"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MfaSetupResponse(false, "Error setting up MFA: " + e.getMessage()));
        }
    }
    
    /**
     * Verify TOTP code and enable MFA
     */
    @PostMapping("/verify")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify MFA", description = "Verify TOTP code and enable MFA")
    public ResponseEntity<MfaSetupResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                       Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            boolean isValid = mfaService.verifyAndEnable(user, request.getCode());
            
            if (isValid) {
                return ResponseEntity.ok(new MfaSetupResponse(
                    true,
                    "MFA enabled successfully. Your account is now secured with two-factor authentication."
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(new MfaSetupResponse(false, "Invalid verification code. Please try again."));
            }
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new MfaSetupResponse(false, "MFA setup not found. Please initiate MFA setup first."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MfaSetupResponse(false, "Error verifying MFA: " + e.getMessage()));
        }
    }
    
    /**
     * Enable MFA (alias for verify endpoint for better API naming)
     */
    @PostMapping("/enable")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Enable MFA", description = "Verify TOTP code and enable MFA (alias for /verify)")
    public ResponseEntity<MfaSetupResponse> enableMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                       Authentication authentication) {
        return verifyMfa(request, authentication);
    }
    
    /**
     * Generate backup codes for account recovery
     */
    @PostMapping("/backup-codes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate Backup Codes", description = "Generate one-time use backup codes for account recovery")
    public ResponseEntity<MfaBackupCodesResponse> generateBackupCodes(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            List<String> backupCodes = mfaService.generateBackupCodes(user);
            
            MfaBackupCodesResponse.MfaBackupCodesData data = 
                new MfaBackupCodesResponse.MfaBackupCodesData(backupCodes);
            
            MfaBackupCodesResponse response = new MfaBackupCodesResponse(
                true,
                "Backup codes generated successfully. Store these codes securely. They can only be used once.",
                data
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new MfaBackupCodesResponse(false, "MFA must be enabled before generating backup codes."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MfaBackupCodesResponse(false, "Error generating backup codes: " + e.getMessage()));
        }
    }
    
    /**
     * Check MFA status for authenticated user
     */
    @GetMapping("/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Check MFA Status", description = "Check if MFA is enabled for the authenticated user")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            boolean enabled = mfaService.isMfaEnabled(user);
            
            MfaStatusResponse.MfaStatusData data = new MfaStatusResponse.MfaStatusData(enabled);
            
            String message = enabled ? "MFA is enabled for this account" : "MFA is not enabled for this account";
            MfaStatusResponse response = new MfaStatusResponse(true, message, data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MfaStatusResponse(false, "Error retrieving MFA status: " + e.getMessage(), null));
        }
    }
    
    /**
     * Disable MFA for authenticated user
     */
    @DeleteMapping("/disable")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Disable MFA", description = "Disable MFA for the authenticated user")
    public ResponseEntity<MfaSetupResponse> disableMfa(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            mfaService.disableMfa(user);
            
            return ResponseEntity.ok(new MfaSetupResponse(
                true,
                "MFA disabled successfully. Your account is no longer protected by two-factor authentication."
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MfaSetupResponse(false, "Error disabling MFA: " + e.getMessage()));
        }
    }
}
