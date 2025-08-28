package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.EmailVerificationToken;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.EmailVerificationTokenRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class EmailVerificationService {
    
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final String TOKEN_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 32;
    
    public String generateVerificationToken(String email) {
        System.out.println("📧 InventSight - Generating verification token for: " + email);
        
        // Clean up any existing tokens for this email
        tokenRepository.deleteAllTokensByEmail(email);
        
        // Generate new token
        String token = generateRandomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, email, expiresAt);
        tokenRepository.save(verificationToken);
        
        // Log activity
        activityLogService.logActivity(
            null,
            "WinKyaw",
            "EMAIL_VERIFICATION_TOKEN_GENERATED",
            "AUTHENTICATION",
            "Verification token generated for email: " + email
        );
        
        System.out.println("✅ Verification token generated successfully for: " + email);
        return token;
    }
    
    public boolean verifyEmail(String token, String email) {
        System.out.println("🔍 InventSight - Verifying email with token for: " + email);
        
        Optional<EmailVerificationToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            System.out.println("❌ Invalid verification token");
            return false;
        }
        
        EmailVerificationToken verificationToken = tokenOptional.get();
        
        if (!verificationToken.getEmail().equals(email)) {
            System.out.println("❌ Token email mismatch");
            return false;
        }
        
        if (!verificationToken.isValid()) {
            System.out.println("❌ Token is expired or already used");
            return false;
        }
        
        // Mark token as used
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);
        
        // Update user email verification status
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Log activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "EMAIL_VERIFIED",
                "AUTHENTICATION",
                "Email verification completed for: " + email
            );
            
            System.out.println("✅ Email verified successfully for: " + email);
            return true;
        } else {
            System.out.println("❌ User not found for email: " + email);
            return false;
        }
    }
    
    public boolean hasValidToken(String email) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByEmail(email, now).isPresent();
    }
    
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = tokenRepository.deleteExpiredAndUsedTokens(now);
        System.out.println("🧹 Cleaned up " + deletedCount + " expired/used verification tokens");
    }
    
    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARACTERS.charAt(random.nextInt(TOKEN_CHARACTERS.length())));
        }
        
        return token.toString();
    }
    
    // For mock email sending (to be replaced with real email service)
    public void sendVerificationEmail(String email, String token) {
        System.out.println("📨 InventSight - Mock sending verification email to: " + email);
        System.out.println("🔗 Verification token: " + token);
        System.out.println("📝 Verification link: http://localhost:3000/verify-email?token=" + token + "&email=" + email);
        
        // Log activity
        activityLogService.logActivity(
            null,
            "WinKyaw",
            "EMAIL_VERIFICATION_SENT",
            "AUTHENTICATION",
            "Verification email sent to: " + email
        );
        
        // In a real implementation, this would integrate with an email service
        // like SendGrid, AWS SES, or SMTP server
        System.out.println("📧 Mock email sent successfully to: " + email);
    }
}