package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_secrets")
public class MfaSecret {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @NotBlank
    @Column(nullable = false)
    private String secret;
    
    @Column(nullable = false)
    private Boolean enabled = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    // New fields for OTP delivery
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_delivery_method", length = 10)
    private DeliveryMethod preferredDeliveryMethod = DeliveryMethod.TOTP;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // Should be encrypted at rest in production
    
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;
    
    // Enum for MFA delivery method
    public enum DeliveryMethod {
        TOTP,  // Time-based One-Time Password (Google Authenticator)
        EMAIL, // OTP sent via email
        SMS    // OTP sent via SMS
    }
    
    // Constructors
    public MfaSecret() {}
    
    public MfaSecret(User user, String secret) {
        this.user = user;
        this.secret = secret;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    
    public DeliveryMethod getPreferredDeliveryMethod() { return preferredDeliveryMethod; }
    public void setPreferredDeliveryMethod(DeliveryMethod preferredDeliveryMethod) { 
        this.preferredDeliveryMethod = preferredDeliveryMethod; 
    }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public Boolean getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }
}
