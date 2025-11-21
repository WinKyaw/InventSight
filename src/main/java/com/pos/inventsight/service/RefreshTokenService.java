package com.pos.inventsight.service;

import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.model.sql.RefreshToken;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.RefreshTokenRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing refresh tokens
 */
@Service
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Value("${inventsight.security.jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenExpirationMs;
    
    /**
     * Create and store a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        System.out.println("🔄 Creating refresh token for user: " + user.getEmail());
        
        // Generate refresh token using JwtUtils
        String tokenString = jwtUtils.generateRefreshToken(user);
        
        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);
        
        // Create refresh token entity
        RefreshToken refreshToken = new RefreshToken(user, tokenString, expiresAt);
        
        // Store IP address and user agent if available
        if (request != null) {
            refreshToken.setIpAddress(getClientIpAddress(request));
            refreshToken.setUserAgent(request.getHeader("User-Agent"));
        }
        
        // Save to database
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        System.out.println("✅ Refresh token created successfully, expires at: " + expiresAt);
        
        return saved;
    }
    
    /**
     * Find refresh token by token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    /**
     * Validate and get refresh token
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        if (refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }
        
        if (refreshToken.isExpired()) {
            throw new RuntimeException("Refresh token has expired");
        }
        
        return refreshToken;
    }
    
    /**
     * Revoke a specific refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        System.out.println("🚫 Revoking refresh token");
        
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        
        System.out.println("✅ Refresh token revoked successfully");
    }
    
    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        System.out.println("🚫 Revoking all refresh tokens for user ID: " + userId);
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        System.out.println("✅ All user refresh tokens revoked");
    }
    
    /**
     * Delete expired tokens - runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        System.out.println("🧹 Cleaning up expired refresh tokens");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.deleteExpiredTokens(now);
            System.out.println("✅ Expired refresh tokens cleaned up");
        } catch (Exception e) {
            System.out.println("❌ Error cleaning up expired tokens: " + e.getMessage());
        }
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Verify refresh token and return associated user
     */
    public User getUserFromRefreshToken(String token) {
        RefreshToken refreshToken = validateRefreshToken(token);
        return refreshToken.getUser();
    }
    
    /**
     * Check if refresh token is valid
     */
    public boolean isTokenValid(String token) {
        return refreshTokenRepository.existsByTokenAndIsValid(token, LocalDateTime.now());
    }
}
