package com.pos.inventsight.controller;

import com.pos.inventsight.dto.MfaVerifyRequest;
import com.pos.inventsight.dto.MfaSetupResponse;
import com.pos.inventsight.dto.MfaBackupCodesResponse;
import com.pos.inventsight.dto.MfaStatusResponse;
import com.pos.inventsight.dto.MfaSendOtpRequest;
import com.pos.inventsight.dto.MfaVerifyOtpRequest;
import com.pos.inventsight.dto.MfaDeliveryMethodRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.MfaSecret;
import com.pos.inventsight.service.MfaService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.RateLimitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RateLimitingService rateLimitingService;
    
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
            
            // Check rate limiting for MFA verification
            if (!rateLimitingService.isMfaVerificationAllowed(user.getEmail())) {
                RateLimitingService.RateLimitStatus rateLimitStatus = 
                    rateLimitingService.getRateLimitStatus("", user.getEmail(), "mfa-verification");
                
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MfaSetupResponse(false, 
                        String.format("Too many verification attempts. Please try again after %s.", 
                            rateLimitStatus.getResetTime())));
            }
            
            // Record verification attempt
            rateLimitingService.recordMfaVerificationAttempt(user.getEmail());
            
            boolean isValid = mfaService.verifyAndEnable(user, request.getCode());
            
            if (isValid) {
                // Clear rate limit attempts on successful verification
                rateLimitingService.clearMfaVerificationAttempts(user.getEmail());
                
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
    
    /**
     * Send OTP code via email or SMS
     */
    @PostMapping("/send-otp")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Send OTP Code", description = "Send OTP code via email or SMS for MFA")
    public ResponseEntity<?> sendOtpCode(@Valid @RequestBody MfaSendOtpRequest request,
                                          Authentication authentication,
                                          HttpServletRequest httpRequest) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            // Convert DTO delivery method to entity enum
            MfaSecret.DeliveryMethod deliveryMethod = request.getDeliveryMethod() == MfaSendOtpRequest.DeliveryMethod.EMAIL
                ? MfaSecret.DeliveryMethod.EMAIL
                : MfaSecret.DeliveryMethod.SMS;
            
            // Get IP address
            String ipAddress = httpRequest.getRemoteAddr();
            
            // Send OTP code
            mfaService.sendOtpCode(user, deliveryMethod, ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP code sent successfully via " + request.getDeliveryMethod());
            response.put("deliveryMethod", request.getDeliveryMethod());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error sending OTP code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Verify OTP code
     */
    @PostMapping("/verify-otp")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify OTP Code", description = "Verify OTP code received via email or SMS")
    public ResponseEntity<?> verifyOtpCode(@Valid @RequestBody MfaVerifyOtpRequest request,
                                            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            // Convert DTO delivery method to entity enum
            MfaSecret.DeliveryMethod deliveryMethod = request.getDeliveryMethod() == MfaVerifyOtpRequest.DeliveryMethod.EMAIL
                ? MfaSecret.DeliveryMethod.EMAIL
                : MfaSecret.DeliveryMethod.SMS;
            
            // Verify OTP code
            boolean isValid = mfaService.verifyOtpCode(user, request.getOtpCode(), deliveryMethod);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", isValid);
            response.put("message", isValid ? "OTP code verified successfully" : "Invalid OTP code");
            
            return isValid ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error verifying OTP code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update MFA delivery method preference
     */
    @PutMapping("/delivery-method")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update MFA Delivery Method", description = "Update preferred MFA delivery method (TOTP/EMAIL/SMS)")
    public ResponseEntity<?> updateDeliveryMethod(@Valid @RequestBody MfaDeliveryMethodRequest request,
                                                    Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            // Convert DTO delivery method to entity enum
            MfaSecret.DeliveryMethod deliveryMethod;
            switch (request.getDeliveryMethod()) {
                case EMAIL:
                    deliveryMethod = MfaSecret.DeliveryMethod.EMAIL;
                    break;
                case SMS:
                    deliveryMethod = MfaSecret.DeliveryMethod.SMS;
                    break;
                default:
                    deliveryMethod = MfaSecret.DeliveryMethod.TOTP;
            }
            
            // Update delivery method
            mfaService.updateDeliveryMethod(user, deliveryMethod, request.getPhoneNumber());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MFA delivery method updated successfully");
            response.put("deliveryMethod", request.getDeliveryMethod());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating delivery method: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Verify phone number for SMS delivery
     */
    @PostMapping("/verify-phone")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify Phone Number", description = "Verify phone number for SMS OTP delivery")
    public ResponseEntity<?> verifyPhoneNumber(@RequestParam("code") String verificationCode,
                                                 Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            // Verify phone number
            mfaService.verifyPhoneNumber(user, verificationCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Phone number verified successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error verifying phone number: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get available MFA delivery methods
     */
    @GetMapping("/delivery-methods")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get MFA Delivery Methods", description = "Get available MFA delivery methods and current preference")
    public ResponseEntity<?> getDeliveryMethods(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            MfaService.MfaStatusDetails status = mfaService.getMfaStatus(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("enabled", status.isEnabled());
            response.put("preferredMethod", status.getPreferredMethod());
            response.put("phoneVerified", status.isPhoneVerified());
            response.put("maskedPhoneNumber", status.getMaskedPhoneNumber());
            response.put("availableMethods", new String[]{"TOTP", "EMAIL", "SMS"});
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving delivery methods: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
