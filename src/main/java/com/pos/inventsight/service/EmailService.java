package com.pos.inventsight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service abstraction with provider toggle support.
 * Supports SMTP, AWS SES, and SendGrid (configurable via properties).
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${inventsight.email.provider:smtp}")
    private String emailProvider;
    
    @Value("${inventsight.email.from-address:noreply@inventsight.com}")
    private String fromAddress;
    
    @Value("${inventsight.email.from-name:InventSight}")
    private String fromName;
    
    /**
     * Send email via configured provider
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            switch (emailProvider.toLowerCase()) {
                case "smtp":
                    sendViaSmtp(to, subject, body);
                    break;
                case "ses":
                    sendViaSes(to, subject, body);
                    break;
                case "sendgrid":
                    sendViaSendgrid(to, subject, body);
                    break;
                default:
                    logger.warn("Unknown email provider: {}. Falling back to SMTP", emailProvider);
                    sendViaSmtp(to, subject, body);
            }
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
    
    /**
     * Send email via SMTP
     */
    private void sendViaSmtp(String to, String subject, String body) {
        if (mailSender == null) {
            logger.error("JavaMailSender not configured - cannot send email");
            throw new IllegalStateException("Email service not configured");
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(String.format("%s <%s>", fromName, fromAddress));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
        logger.info("Email sent via SMTP to: {}", to);
    }
    
    /**
     * Send email via AWS SES (stub - implement with AWS SDK)
     */
    private void sendViaSes(String to, String subject, String body) {
        // TODO: Implement AWS SES integration
        // Requires: software.amazon.awssdk:ses dependency
        logger.info("SES email sending not yet implemented - using SMTP fallback");
        sendViaSmtp(to, subject, body);
    }
    
    /**
     * Send email via SendGrid (stub - implement with SendGrid SDK)
     */
    private void sendViaSendgrid(String to, String subject, String body) {
        // TODO: Implement SendGrid integration
        // Requires: com.sendgrid:sendgrid-java dependency
        logger.info("SendGrid email sending not yet implemented - using SMTP fallback");
        sendViaSmtp(to, subject, body);
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Password Reset Request - InventSight";
        String body = String.format(
                "Hello,\n\n" +
                "You requested a password reset for your InventSight account.\n\n" +
                "Click the link below to reset your password:\n%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request this reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The InventSight Team",
                resetLink
        );
        
        sendEmail(to, subject, body);
    }
    
    /**
     * Send MFA setup email
     */
    public void sendMfaSetupEmail(String to, String userName) {
        String subject = "Multi-Factor Authentication Enabled - InventSight";
        String body = String.format(
                "Hello %s,\n\n" +
                "Multi-factor authentication has been successfully enabled for your InventSight account.\n\n" +
                "Your account is now more secure. You will need to enter a verification code from your authenticator app when you log in.\n\n" +
                "If you did not enable MFA, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "The InventSight Team",
                userName
        );
        
        sendEmail(to, subject, body);
    }
}
