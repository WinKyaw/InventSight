package com.pos.inventsight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailVerificationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    // Constructors
    public EmailVerificationRequest() {}
    
    public EmailVerificationRequest(String token, String email) {
        this.token = token;
        this.email = email;
    }
    
    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    @Override
    public String toString() {
        return "EmailVerificationRequest{" +
                "email='" + email + '\'' +
                ", token='[PROTECTED]'" +
                '}';
    }
}