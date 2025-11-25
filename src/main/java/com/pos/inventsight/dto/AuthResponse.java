package com.pos.inventsight.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String system;
    private LocalDateTime timestamp;
    private String message;
    private Long expiresIn; // Token expiration time in milliseconds
    
    // Constructor for successful authentication
    public AuthResponse(String accessToken, UUID id, String username, String email, 
                       String fullName, String role, String system, Long expiresIn) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.system = system;
        this.expiresIn = expiresIn;
        this.timestamp = LocalDateTime.now();
        this.message = "Authentication successful";
    }
    
    // Constructor for simple responses (logout, etc.)
    public AuthResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.system = "InventSight";
    }
    
    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getTokenType() { return type; }
    public void setTokenType(String type) { this.type = type; }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
}