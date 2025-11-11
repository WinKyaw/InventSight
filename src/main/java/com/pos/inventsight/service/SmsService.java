package com.pos.inventsight.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS messages using Twilio.
 * Supports phone number validation and formatting.
 */
@Service
public class SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    
    @Value("${inventsight.sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${inventsight.sms.provider:twilio}")
    private String smsProvider;
    
    @Value("${inventsight.sms.twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${inventsight.sms.twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${inventsight.sms.twilio.from-number:}")
    private String twilioFromNumber;
    
    private boolean twilioInitialized = false;
    
    /**
     * Initialize Twilio client
     */
    private void initializeTwilio() {
        if (!twilioInitialized && smsEnabled && "twilio".equalsIgnoreCase(smsProvider)) {
            if (twilioAccountSid == null || twilioAccountSid.isEmpty() ||
                twilioAuthToken == null || twilioAuthToken.isEmpty()) {
                logger.warn("Twilio credentials not configured. SMS functionality will be disabled.");
                return;
            }
            
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                twilioInitialized = true;
                logger.info("Twilio SMS service initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Send SMS message
     */
    public boolean sendSms(String toPhoneNumber, String messageText) {
        if (!smsEnabled) {
            logger.warn("SMS service is disabled");
            return false;
        }
        
        initializeTwilio();
        
        if (!twilioInitialized) {
            logger.error("Twilio not initialized. Cannot send SMS.");
            return false;
        }
        
        try {
            // Validate phone number
            if (!isValidPhoneNumber(toPhoneNumber)) {
                logger.error("Invalid phone number format: {}", toPhoneNumber);
                return false;
            }
            
            Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(twilioFromNumber),
                messageText
            ).create();
            
            logger.info("SMS sent successfully to {} with SID: {}", toPhoneNumber, message.getSid());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send OTP code via SMS
     */
    public boolean sendOtpCode(String toPhoneNumber, String otpCode) {
        String message = String.format(
            "Your InventSight verification code is: %s\n\nThis code will expire in 5 minutes.\n\nIf you didn't request this code, please ignore this message.",
            otpCode
        );
        return sendSms(toPhoneNumber, message);
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        try {
            // Parse phone number (assuming international format)
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            logger.warn("Invalid phone number: {}", phoneNumber);
            return false;
        }
    }
    
    /**
     * Format phone number to E.164 format
     */
    public String formatPhoneNumber(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            logger.warn("Failed to format phone number: {}", phoneNumber);
            return phoneNumber;
        }
    }
    
    /**
     * Parse and validate phone number with country code
     */
    public String parseAndValidatePhoneNumber(String phoneNumber, String defaultRegion) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, defaultRegion);
            
            if (!phoneNumberUtil.isValidNumber(number)) {
                throw new IllegalArgumentException("Invalid phone number: " + phoneNumber);
            }
            
            return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Failed to parse phone number: " + phoneNumber, e);
        }
    }
    
    /**
     * Check if SMS service is enabled and configured
     */
    public boolean isSmsEnabled() {
        return smsEnabled && twilioAccountSid != null && !twilioAccountSid.isEmpty();
    }
}
