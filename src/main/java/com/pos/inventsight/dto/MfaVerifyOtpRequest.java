package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for verifying OTP code
 */
public class MfaVerifyOtpRequest {
    
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;
    
    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;
    
    public enum DeliveryMethod {
        EMAIL,
        SMS
    }
    
    // Constructors
    public MfaVerifyOtpRequest() {}
    
    public MfaVerifyOtpRequest(String otpCode, DeliveryMethod deliveryMethod) {
        this.otpCode = otpCode;
        this.deliveryMethod = deliveryMethod;
    }
    
    // Getters and Setters
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }
}
