package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.PasswordResetToken;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.PasswordResetTokenRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling password reset flow with email delivery.
 * Tokens are signed, single-use, and time-limited.
 */
@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_EXPIRY_HOURS = 1;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Request a password reset - generates token and sends email
     */
    @Transactional
    public void requestPasswordReset(String email, String resetBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            // Don't reveal if user exists - just log and return
            logger.info("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOpt.get();
        
        // Generate secure token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        
        // Get request metadata
        String ipAddress = null;
        String userAgent = null;
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }
        
        // Create and save token
        PasswordResetToken resetToken = new PasswordResetToken(user, token, expiryDate);
        resetToken.setIpAddress(ipAddress);
        resetToken.setUserAgent(userAgent);
        tokenRepository.save(resetToken);
        
        // Send email
        String resetLink = String.format("%s?token=%s", resetBaseUrl, token);
        emailService.sendPasswordResetEmail(email, resetLink);
        
        logger.info("Password reset requested for user: {}", email);
        auditService.logAsync(email, user.getUuid(), "PASSWORD_RESET_REQUESTED", "User", user.getId().toString(), null);
    }
    
    /**
     * Validate password reset token
     */
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
        
        if (resetTokenOpt.isEmpty()) {
            return false;
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        return resetToken.isValid();
    }
    
    /**
     * Reset password using token
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
        
        if (resetTokenOpt.isEmpty()) {
            logger.warn("Password reset attempted with invalid token");
            return false;
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        
        if (!resetToken.isValid()) {
            logger.warn("Password reset attempted with expired or used token");
            return false;
        }
        
        User user = resetToken.getUser();
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);
        
        logger.info("Password reset successful for user: {}", user.getEmail());
        auditService.logAsync(user.getEmail(), user.getUuid(), "PASSWORD_RESET_COMPLETED", "User", user.getId().toString(), null);
        
        return true;
    }
    
    /**
     * Clean up expired tokens (should be scheduled)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.debug("Expired password reset tokens cleaned up");
    }
}
