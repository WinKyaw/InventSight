package com.pos.inventsight.dto;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String system;
    
    public LoginResponse(String accessToken, Long id, String username, String email, String fullName, String role, String system) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.system = system;
    }
    
    // Getters
    public String getAccessToken() { return token; }
    public String getTokenType() { return type; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getSystem() { return system; }
}