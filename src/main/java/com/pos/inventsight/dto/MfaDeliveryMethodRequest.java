package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating MFA delivery method preference
 */
public class MfaDeliveryMethodRequest {
    
    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;
    
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", 
             message = "Phone number must be in E.164 format (e.g., +1234567890)")
    private String phoneNumber; // Required if deliveryMethod is SMS
    
    public enum DeliveryMethod {
        TOTP,
        EMAIL,
        SMS
    }
    
    // Constructors
    public MfaDeliveryMethodRequest() {}
    
    public MfaDeliveryMethodRequest(DeliveryMethod deliveryMethod, String phoneNumber) {
        this.deliveryMethod = deliveryMethod;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
