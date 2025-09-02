package com.pos.inventsight.dto;

import java.time.LocalDateTime;

/**
 * Token response DTO that matches frontend expectations
 * Contains both access and refresh tokens
 */
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // Access token expiration in milliseconds
    private Long refreshExpiresIn; // Refresh token expiration in milliseconds
    private LocalDateTime issuedAt;
    
    public TokenResponse(String accessToken, String refreshToken, Long expiresIn, Long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.issuedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    
    public Long getRefreshExpiresIn() { return refreshExpiresIn; }
    public void setRefreshExpiresIn(Long refreshExpiresIn) { this.refreshExpiresIn = refreshExpiresIn; }
    
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}