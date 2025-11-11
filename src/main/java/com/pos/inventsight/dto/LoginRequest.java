package com.pos.inventsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    // Optional tenant ID for offline mode to issue tenant-bound JWTs
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "Tenant ID must be a valid UUID format")
    private String tenantId;
    
    // Optional TOTP code for MFA (required when user has MFA enabled)
    private Integer totpCode;
    
    // Optional OTP code for email/SMS MFA
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;
    
    // Optional MFA method override (to use specific method instead of user's preference)
    private String mfaMethod; // TOTP, EMAIL, or SMS
    
    // Constructors
    public LoginRequest() {}
    
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public LoginRequest(String email, String password, String tenantId) {
        this.email = email;
        this.password = password;
        this.tenantId = tenantId;
    }
    
    public LoginRequest(String email, String password, String tenantId, Integer totpCode) {
        this.email = email;
        this.password = password;
        this.tenantId = tenantId;
        this.totpCode = totpCode;
    }
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public Integer getTotpCode() { return totpCode; }
    public void setTotpCode(Integer totpCode) { this.totpCode = totpCode; }
    
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    
    public String getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(String mfaMethod) { this.mfaMethod = mfaMethod; }
    
    @Override
    public String toString() {
        return "InventSight LoginRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", tenantId='" + (tenantId != null ? tenantId : "none") + '\'' +
                ", totpCode='" + (totpCode != null ? "[PROVIDED]" : "none") + '\'' +
                ", otpCode='" + (otpCode != null ? "[PROVIDED]" : "none") + '\'' +
                ", mfaMethod='" + (mfaMethod != null ? mfaMethod : "none") + '\'' +
                '}';
    }
}