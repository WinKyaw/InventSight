package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a One-Time Password (OTP) code for MFA authentication.
 * Supports both Email and SMS delivery methods.
 */
@Entity
@Table(name = "otp_codes", indexes = {
    @Index(name = "idx_otp_user_id", columnList = "user_id"),
    @Index(name = "idx_otp_expires_at", columnList = "expires_at"),
    @Index(name = "idx_otp_verified", columnList = "verified")
})
public class OtpCode {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(nullable = false, length = 255)
    private String code; // BCrypt hashed OTP code
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 10)
    private DeliveryMethod deliveryMethod;
    
    @NotBlank
    @Column(name = "sent_to", nullable = false)
    private String sentTo; // Email address or phone number
    
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @NotNull
    @Column(nullable = false)
    private Boolean verified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    // Enum for delivery method
    public enum DeliveryMethod {
        EMAIL,
        SMS
    }
    
    // Constructors
    public OtpCode() {}
    
    public OtpCode(User user, String hashedCode, DeliveryMethod deliveryMethod, 
                   String sentTo, LocalDateTime expiresAt, String ipAddress) {
        this.user = user;
        this.code = hashedCode;
        this.deliveryMethod = deliveryMethod;
        this.sentTo = sentTo;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.verified = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    
    public String getSentTo() { return sentTo; }
    public void setSentTo(String sentTo) { this.sentTo = sentTo; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !verified && !isExpired();
    }
    
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
