package com.pos.inventsight.dto;

import java.time.LocalDateTime;

/**
 * Structured auth response DTO that matches frontend expectations
 * Contains nested user and tokens objects
 */
public class StructuredAuthResponse {
    private UserResponse user;
    private TokenResponse tokens;
    private String message;
    private boolean success;
    private LocalDateTime timestamp;
    private String system;
    
    public StructuredAuthResponse(UserResponse user, TokenResponse tokens, String message) {
        this.user = user;
        this.tokens = tokens;
        this.message = message;
        this.success = true;
        this.timestamp = LocalDateTime.now();
        this.system = "InventSight";
    }
    
    // Constructor for error responses
    public StructuredAuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
        this.system = "InventSight";
    }
    
    // Getters and Setters
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    
    public TokenResponse getTokens() { return tokens; }
    public void setTokens(TokenResponse tokens) { this.tokens = tokens; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
}