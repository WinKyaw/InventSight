package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.MfaBackupCode;
import com.pos.inventsight.model.sql.MfaSecret;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.OtpCode;
import com.pos.inventsight.repository.sql.MfaBackupCodeRepository;
import com.pos.inventsight.repository.sql.MfaSecretRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
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
        
        // Generate QR code image (Base64 encoded PNG)
        String qrCodeImage = null;
        try {
            qrCodeImage = generateQrCodeImage(qrCodeUrl, 300, 300);
        } catch (Exception e) {
            logger.warn("Failed to generate QR code image for user: {}", user.getEmail(), e);
        }
        
        logger.info("MFA setup initiated for user: {}", user.getEmail());
        auditService.logAsync(user.getEmail(), user.getId(), "MFA_SETUP_INITIATED", "User", user.getId().toString(), null);
        
        return new MfaSetupResponse(secret, qrCodeUrl, qrCodeImage);
    }
    
    /**
     * Generate QR code image as Base64 encoded PNG
     */
    private String generateQrCodeImage(String text, int width, int height) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
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
     * Send OTP code via email or SMS
     */
    @Transactional
    public void sendOtpCode(User user, MfaSecret.DeliveryMethod deliveryMethod, String ipAddress) {
        // Get or create MFA secret
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user).orElse(null);
        if (mfaSecret == null || !mfaSecret.getEnabled()) {
            throw new IllegalStateException("MFA is not enabled for this user");
        }
        
        String sentTo;
        OtpCode.DeliveryMethod otpDeliveryMethod;
        
        if (deliveryMethod == MfaSecret.DeliveryMethod.EMAIL) {
            sentTo = user.getEmail();
            otpDeliveryMethod = OtpCode.DeliveryMethod.EMAIL;
        } else if (deliveryMethod == MfaSecret.DeliveryMethod.SMS) {
            if (mfaSecret.getPhoneNumber() == null || !mfaSecret.getPhoneVerified()) {
                throw new IllegalStateException("Phone number not verified for SMS delivery");
            }
            sentTo = mfaSecret.getPhoneNumber();
            otpDeliveryMethod = OtpCode.DeliveryMethod.SMS;
        } else {
            throw new IllegalArgumentException("Invalid delivery method for OTP: " + deliveryMethod);
        }
        
        // Generate OTP code
        OtpCode otpCode = otpService.createOtpCode(user, otpDeliveryMethod, sentTo, ipAddress);
        String plainCode = otpCode.getCode(); // This is the plain code before it was hashed
        
        // Send OTP code
        if (deliveryMethod == MfaSecret.DeliveryMethod.EMAIL) {
            emailService.sendLoginOtpCode(sentTo, plainCode);
        } else if (deliveryMethod == MfaSecret.DeliveryMethod.SMS) {
            smsService.sendOtpCode(sentTo, plainCode);
        }
        
        logger.info("OTP code sent via {} to user: {}", deliveryMethod, user.getEmail());
    }
    
    /**
     * Verify OTP code for login
     */
    @Transactional
    public boolean verifyOtpCode(User user, String otpCode, MfaSecret.DeliveryMethod deliveryMethod) {
        OtpCode.DeliveryMethod otpDeliveryMethod = deliveryMethod == MfaSecret.DeliveryMethod.EMAIL 
            ? OtpCode.DeliveryMethod.EMAIL 
            : OtpCode.DeliveryMethod.SMS;
        
        return otpService.verifyOtpCode(user, otpCode, otpDeliveryMethod);
    }
    
    /**
     * Get user's preferred MFA delivery method
     */
    @Transactional(readOnly = true)
    public MfaSecret.DeliveryMethod getPreferredDeliveryMethod(User user) {
        return mfaSecretRepository.findByUser(user)
                .map(MfaSecret::getPreferredDeliveryMethod)
                .orElse(MfaSecret.DeliveryMethod.TOTP);
    }
    
    /**
     * Update user's preferred MFA delivery method
     */
    @Transactional
    public void updateDeliveryMethod(User user, MfaSecret.DeliveryMethod deliveryMethod, String phoneNumber) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA not set up for this user"));
        
        if (!mfaSecret.getEnabled()) {
            throw new IllegalStateException("MFA must be enabled before changing delivery method");
        }
        
        // Validate phone number for SMS
        if (deliveryMethod == MfaSecret.DeliveryMethod.SMS) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                throw new IllegalArgumentException("Phone number required for SMS delivery");
            }
            
            // Validate and format phone number
            String formattedPhone = smsService.formatPhoneNumber(phoneNumber);
            if (!smsService.isValidPhoneNumber(formattedPhone)) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
            
            mfaSecret.setPhoneNumber(formattedPhone);
            mfaSecret.setPhoneVerified(false); // Require re-verification
        }
        
        mfaSecret.setPreferredDeliveryMethod(deliveryMethod);
        mfaSecretRepository.save(mfaSecret);
        
        logger.info("MFA delivery method updated to {} for user: {}", deliveryMethod, user.getEmail());
        auditService.logAsync(user.getEmail(), user.getId(), "MFA_DELIVERY_METHOD_UPDATED", 
            "User", user.getId().toString(), "Delivery method: " + deliveryMethod);
    }
    
    /**
     * Verify phone number for SMS delivery
     */
    @Transactional
    public void verifyPhoneNumber(User user, String verificationCode) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA not set up for this user"));
        
        if (mfaSecret.getPhoneNumber() == null) {
            throw new IllegalStateException("No phone number registered");
        }
        
        // Verify OTP code sent to phone
        boolean isValid = otpService.verifyOtpCode(user, verificationCode, OtpCode.DeliveryMethod.SMS);
        
        if (isValid) {
            mfaSecret.setPhoneVerified(true);
            mfaSecretRepository.save(mfaSecret);
            
            logger.info("Phone number verified for user: {}", user.getEmail());
            auditService.logAsync(user.getEmail(), user.getId(), "PHONE_VERIFIED", 
                "User", user.getId().toString(), null);
        } else {
            throw new IllegalArgumentException("Invalid verification code");
        }
    }
    
    /**
     * Get MFA status including delivery method
     */
    @Transactional(readOnly = true)
    public MfaStatusDetails getMfaStatus(User user) {
        Optional<MfaSecret> mfaSecretOpt = mfaSecretRepository.findByUser(user);
        
        if (mfaSecretOpt.isEmpty()) {
            return new MfaStatusDetails(false, null, false, null);
        }
        
        MfaSecret mfaSecret = mfaSecretOpt.get();
        return new MfaStatusDetails(
            mfaSecret.getEnabled(),
            mfaSecret.getPreferredDeliveryMethod(),
            mfaSecret.getPhoneVerified(),
            mfaSecret.getPhoneNumber() != null ? maskPhoneNumber(mfaSecret.getPhoneNumber()) : null
        );
    }
    
    /**
     * Mask phone number for display (show only last 4 digits)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    /**
     * Response DTO for MFA status with delivery method
     */
    public static class MfaStatusDetails {
        private final boolean enabled;
        private final MfaSecret.DeliveryMethod preferredMethod;
        private final boolean phoneVerified;
        private final String maskedPhoneNumber;
        
        public MfaStatusDetails(boolean enabled, MfaSecret.DeliveryMethod preferredMethod, 
                               boolean phoneVerified, String maskedPhoneNumber) {
            this.enabled = enabled;
            this.preferredMethod = preferredMethod;
            this.phoneVerified = phoneVerified;
            this.maskedPhoneNumber = maskedPhoneNumber;
        }
        
        public boolean isEnabled() { return enabled; }
        public MfaSecret.DeliveryMethod getPreferredMethod() { return preferredMethod; }
        public boolean isPhoneVerified() { return phoneVerified; }
        public String getMaskedPhoneNumber() { return maskedPhoneNumber; }
    }
    
    /**
     * Response DTO for MFA setup
     */
    public static class MfaSetupResponse {
        private final String secret;
        private final String qrCodeUrl;
        private final String qrCodeImage; // Base64 encoded PNG
        
        public MfaSetupResponse(String secret, String qrCodeUrl, String qrCodeImage) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
            this.qrCodeImage = qrCodeImage;
        }
        
        public String getSecret() { return secret; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public String getQrCodeImage() { return qrCodeImage; }
    }
}
