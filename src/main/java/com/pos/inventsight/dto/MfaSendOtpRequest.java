package com.pos.inventsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for sending OTP code via email or SMS
 */
public class MfaSendOtpRequest {
    
    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;
    
    @Email(message = "Invalid email format")
    private String email; // Required if deliveryMethod is EMAIL
    
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", 
             message = "Phone number must be in E.164 format (e.g., +1234567890)")
    private String phoneNumber; // Required if deliveryMethod is SMS
    
    public enum DeliveryMethod {
        EMAIL,
        SMS
    }
    
    // Constructors
    public MfaSendOtpRequest() {}
    
    public MfaSendOtpRequest(DeliveryMethod deliveryMethod, String email, String phoneNumber) {
        this.deliveryMethod = deliveryMethod;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
