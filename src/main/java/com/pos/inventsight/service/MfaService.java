package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.MfaBackupCode;
import com.pos.inventsight.model.sql.MfaSecret;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.MfaBackupCodeRepository;
import com.pos.inventsight.repository.MfaSecretRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Multi-Factor Authentication (MFA) using TOTP.
 * Supports Google Authenticator compatible QR codes and backup codes.
 */
@Service
public class MfaService {
    
    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final String BACKUP_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing chars
    
    @Autowired
    private MfaSecretRepository mfaSecretRepository;
    
    @Autowired
    private MfaBackupCodeRepository mfaBackupCodeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditService auditService;
    
    @Value("${inventsight.mfa.issuer:InventSight}")
    private String issuer;
    
    @Value("${inventsight.mfa.totp.window-size:3}")
    private int windowSize;
    
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    
    /**
     * Setup MFA for a user - generates secret and QR code
     */
    @Transactional
    public MfaSetupResponse setupMfa(User user) {
        // Check if MFA is already enabled
        Optional<MfaSecret> existing = mfaSecretRepository.findByUser(user);
        if (existing.isPresent() && existing.get().getEnabled()) {
            throw new IllegalStateException("MFA is already enabled for this user");
        }
        
        // Generate new secret
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        
        // Create or update MFA secret
        MfaSecret mfaSecret = existing.orElse(new MfaSecret(user, secret));
        mfaSecret.setSecret(secret);
        mfaSecret.setEnabled(false); // Will be enabled after verification
        mfaSecret = mfaSecretRepository.save(mfaSecret);
        
        // Generate QR code URL
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                issuer,
                user.getEmail(),
                key
        );
        
        logger.info("MFA setup initiated for user: {}", user.getEmail());
        auditService.logAsync(user.getEmail(), user.getId(), "MFA_SETUP_INITIATED", "User", user.getId().toString(), null);
        
        return new MfaSetupResponse(secret, qrCodeUrl);
    }
    
    /**
     * Verify TOTP code and enable MFA if valid
     */
    @Transactional
    public boolean verifyAndEnable(User user, int code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA not set up for this user"));
        
        // Verify the code
        boolean isValid = gAuth.authorize(mfaSecret.getSecret(), code, windowSize);
        
        if (isValid) {
            mfaSecret.setEnabled(true);
            mfaSecret.setVerifiedAt(LocalDateTime.now());
            mfaSecretRepository.save(mfaSecret);
            
            logger.info("MFA enabled for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), "MFA_ENABLED", "User", user.getId().toString(), null);
            
            return true;
        }
        
        logger.warn("Invalid MFA verification code for user: {}", user.getEmail());
        return false;
    }
    
    /**
     * Verify TOTP code for login (when MFA is already enabled)
     */
    @Transactional(readOnly = true)
    public boolean verifyCode(User user, int code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElse(null);
        
        if (mfaSecret == null || !mfaSecret.getEnabled()) {
            return false;
        }
        
        boolean isValid = gAuth.authorize(mfaSecret.getSecret(), code, windowSize);
        
        if (isValid) {
            logger.debug("MFA code verified for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), "MFA_VERIFIED", "User", user.getId().toString(), null);
        } else {
            logger.warn("Invalid MFA code for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), "MFA_FAILED", "User", user.getId().toString(), null);
        }
        
        return isValid;
    }
    
    /**
     * Generate backup codes for account recovery
     */
    @Transactional
    public List<String> generateBackupCodes(User user) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA not set up for this user"));
        
        if (!mfaSecret.getEnabled()) {
            throw new IllegalStateException("MFA must be enabled before generating backup codes");
        }
        
        // Delete existing backup codes
        mfaBackupCodeRepository.deleteByUser(user);
        
        // Generate new backup codes
        List<String> plainCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = generateBackupCode(random);
            plainCodes.add(code);
            
            // Store hashed version
            String hashedCode = passwordEncoder.encode(code);
            MfaBackupCode backupCode = new MfaBackupCode(user, hashedCode);
            mfaBackupCodeRepository.save(backupCode);
        }
        
        logger.info("Generated {} backup codes for user: {}", BACKUP_CODE_COUNT, user.getEmail());
        auditService.logAsync(user.getEmail(), user.getId(), "MFA_BACKUP_CODES_GENERATED", "User", user.getId().toString(), null);
        
        return plainCodes;
    }
    
    /**
     * Generate a single backup code
     */
    private String generateBackupCode(SecureRandom random) {
        StringBuilder code = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            int index = random.nextInt(BACKUP_CODE_CHARS.length());
            code.append(BACKUP_CODE_CHARS.charAt(index));
        }
        return code.toString();
    }
    
    /**
     * Verify backup code (one-time use)
     */
    @Transactional
    public boolean verifyBackupCode(User user, String code) {
        List<MfaBackupCode> backupCodes = mfaBackupCodeRepository.findByUserAndUsed(user, false);
        
        for (MfaBackupCode backupCode : backupCodes) {
            if (passwordEncoder.matches(code, backupCode.getCodeHash())) {
                backupCode.markAsUsed();
                mfaBackupCodeRepository.save(backupCode);
                
                logger.info("Backup code used for user: {}", user.getEmail());
                auditService.logAsync(user.getEmail(), user.getId(), "MFA_BACKUP_CODE_USED", "User", user.getId().toString(), null);
                
                return true;
            }
        }
        
        logger.warn("Invalid backup code for user: {}", user.getEmail());
        return false;
    }
    
    /**
     * Check if MFA is enabled for user
     */
    @Transactional(readOnly = true)
    public boolean isMfaEnabled(User user) {
        return mfaSecretRepository.findByUser(user)
                .map(MfaSecret::getEnabled)
                .orElse(false);
    }
    
    /**
     * Disable MFA for user
     */
    @Transactional
    public void disableMfa(User user) {
        mfaSecretRepository.findByUser(user).ifPresent(mfaSecret -> {
            mfaSecret.setEnabled(false);
            mfaSecretRepository.save(mfaSecret);
            
            // Delete backup codes
            mfaBackupCodeRepository.deleteByUser(user);
            
            logger.info("MFA disabled for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), "MFA_DISABLED", "User", user.getId().toString(), null);
        });
    }
    
    /**
     * Response DTO for MFA setup
     */
    public static class MfaSetupResponse {
        private final String secret;
        private final String qrCodeUrl;
        
        public MfaSetupResponse(String secret, String qrCodeUrl) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
        }
        
        public String getSecret() { return secret; }
        public String getQrCodeUrl() { return qrCodeUrl; }
    }
}
