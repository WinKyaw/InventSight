package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.OtpCode;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing One-Time Password (OTP) codes for MFA authentication.
 * Supports generation, validation, and rate limiting of OTP codes.
 */
@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditService auditService;
    
    @Value("${inventsight.mfa.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;
    
    @Value("${inventsight.mfa.otp.code-length:6}")
    private int otpCodeLength;
    
    @Value("${inventsight.mfa.otp.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${inventsight.mfa.otp.rate-limit-window-minutes:10}")
    private int rateLimitWindowMinutes;
    
    @Value("${inventsight.mfa.otp.max-sends-per-window:3}")
    private int maxSendsPerWindow;
    
    /**
     * Generate a new 6-digit OTP code
     */
    public String generateOtpCode() {
        int bound = (int) Math.pow(10, otpCodeLength);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + otpCodeLength + "d", code);
    }
    
    /**
     * Create and store a new OTP code for a user
     */
    @Transactional
    public OtpCode createOtpCode(User user, OtpCode.DeliveryMethod deliveryMethod, 
                                  String sentTo, String ipAddress) {
        // Check rate limiting
        if (!isRateLimitAllowed(user)) {
            logger.warn("OTP rate limit exceeded for user: {}", user.getEmail());
            throw new IllegalStateException("Too many OTP requests. Please try again later.");
        }
        
        // Generate new OTP code
        String plainCode = generateOtpCode();
        String hashedCode = passwordEncoder.encode(plainCode);
        
        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        
        // Create and save OTP code
        OtpCode otpCode = new OtpCode(user, hashedCode, deliveryMethod, sentTo, expiresAt, ipAddress);
        otpCode = otpCodeRepository.save(otpCode);
        
        logger.info("OTP code generated for user: {} via {}", user.getEmail(), deliveryMethod);
        auditService.logAsync(user.getEmail(), user.getId(), 
            "OTP_GENERATED", "MFA", user.getId().toString(), 
            "OTP code generated via " + deliveryMethod);
        
        // Store plain code temporarily (return it so it can be sent)
        // In production, this should be sent immediately and not stored
        otpCode.setCode(plainCode); // Temporarily set plain code for sending
        return otpCode;
    }
    
    /**
     * Verify an OTP code for a user
     */
    @Transactional
    public boolean verifyOtpCode(User user, String otpCode, OtpCode.DeliveryMethod deliveryMethod) {
        if (otpCode == null || otpCode.length() != otpCodeLength) {
            logger.warn("Invalid OTP code format for user: {}", user.getEmail());
            return false;
        }
        
        // Find the latest valid OTP code for the user
        Optional<OtpCode> otpOptional = otpCodeRepository.findLatestValidOtpByUser(user, LocalDateTime.now());
        
        if (otpOptional.isEmpty()) {
            logger.warn("No valid OTP code found for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), 
                "OTP_VERIFY_FAILED", "MFA", user.getId().toString(), 
                "No valid OTP code found");
            return false;
        }
        
        OtpCode storedOtp = otpOptional.get();
        
        // Verify delivery method matches
        if (storedOtp.getDeliveryMethod() != deliveryMethod) {
            logger.warn("OTP delivery method mismatch for user: {}", user.getEmail());
            return false;
        }
        
        // Verify the code
        boolean isValid = passwordEncoder.matches(otpCode, storedOtp.getCode());
        
        if (isValid) {
            // Mark as verified
            storedOtp.markAsVerified();
            otpCodeRepository.save(storedOtp);
            
            logger.info("OTP code verified successfully for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), 
                "OTP_VERIFIED", "MFA", user.getId().toString(), 
                "OTP code verified via " + deliveryMethod);
        } else {
            logger.warn("Invalid OTP code provided for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), 
                "OTP_VERIFY_FAILED", "MFA", user.getId().toString(), 
                "Invalid OTP code provided");
        }
        
        return isValid;
    }
    
    /**
     * Check if user can request a new OTP (rate limiting)
     */
    public boolean isRateLimitAllowed(User user) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long count = otpCodeRepository.countOtpsByUserSince(user, since);
        return count < maxSendsPerWindow;
    }
    
    /**
     * Get the number of OTP requests remaining for a user
     */
    public int getRemainingOtpRequests(User user) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long count = otpCodeRepository.countOtpsByUserSince(user, since);
        return Math.max(0, maxSendsPerWindow - (int) count);
    }
    
    /**
     * Invalidate all pending OTP codes for a user
     */
    @Transactional
    public void invalidateUserOtpCodes(User user) {
        List<OtpCode> pendingCodes = otpCodeRepository.findOtpsByUserSince(
            user, LocalDateTime.now().minusMinutes(otpExpiryMinutes));
        
        for (OtpCode otp : pendingCodes) {
            if (!otp.getVerified()) {
                otp.setVerified(true);
                otp.setVerifiedAt(LocalDateTime.now());
            }
        }
        
        if (!pendingCodes.isEmpty()) {
            otpCodeRepository.saveAll(pendingCodes);
            logger.info("Invalidated {} pending OTP codes for user: {}", 
                pendingCodes.size(), user.getEmail());
        }
    }
    
    /**
     * Cleanup expired OTP codes (scheduled job)
     */
    @Transactional
    public void cleanupExpiredOtpCodes() {
        otpCodeRepository.deleteExpiredOtps(LocalDateTime.now());
        logger.info("Cleaned up expired OTP codes");
    }
    
    /**
     * Get OTP expiry time in minutes
     */
    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }
}
