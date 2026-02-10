package com.pos.inventsight.config;

import com.pos.inventsight.model.sql.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.util.Date;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${inventsight.security.jwt.secret:inventsight-super-secret-jwt-key-change-in-production-2025}")
    private String jwtSecret;
    
    @Value("${inventsight.security.jwt.expiration:86400000}")
    private int jwtExpirationMs;
    
    @Value("${inventsight.security.jwt.refresh-expiration:604800000}")
    private int jwtRefreshExpirationMs; // 7 days
    
    /**
     * Validate JWT configuration on startup
     * Ensures JWT secret is properly configured and meets minimum security requirements
     */
    @PostConstruct
    public void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret is not configured. Please set 'inventsight.security.jwt.secret' in application.yml " +
                "or provide JWT_SECRET environment variable."
            );
        }
        
        if (jwtSecret.length() < 32) {
            logger.warn("JWT secret is shorter than recommended minimum length. " +
                "Please use a longer secret for production environments.");
        }
        
        logger.info("JWT configuration validated successfully");
        logger.debug("Access token expiration: {} minutes", jwtExpirationMs / 1000 / 60);
        logger.debug("Refresh token expiration: {} days", jwtRefreshExpirationMs / 1000 / 60 / 60 / 24);
    }
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateJwtToken(User user) {
        return generateJwtToken(user, null);
    }
    
    /**
     * Generate JWT token with tenant_id claim (required for production)
     */
    public String generateJwtToken(User user, String tenantId) {
        // Validate tenant_id is provided
        if (tenantId == null || tenantId.isEmpty()) {
            logger.error("Attempting to generate JWT without tenant_id for user: {}", user.getEmail());
            throw new IllegalArgumentException("tenant_id is required for JWT generation");
        }
        
        logger.debug("Generating JWT token with tenant_id: {} for user: {}", tenantId, user.getEmail());
        
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("system", "InventSight")
                .claim("createdBy", "WinKyaw")
                .claim("tokenType", "access")
                .claim("tenant_id", tenantId)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs));
        
        return builder.signWith(getSigningKey()).compact();
    }
    
    public String generateRefreshToken(User user) {
        logger.debug("Generating refresh token for user: {}", user.getEmail());
        
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("system", "InventSight")
                .claim("tokenType", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "refresh".equals(claims.get("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public Long getUserIdFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("userId", Long.class);
    }
    
    public String getFullNameFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("fullName", String.class);
    }
    
    public String getRoleFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("role", String.class);
    }
    
    /**
     * Get tenant_id from JWT token
     */
    public String getTenantIdFromJwtToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("tenant_id", String.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get company_id from JWT token
     * Returns the company_id claim if present in the token
     */
    public String getCompanyIdFromJwtToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("company_id", String.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if token contains tenant_id claim
     */
    public boolean hasTenantId(String token) {
        String tenantId = getTenantIdFromJwtToken(token);
        return tenantId != null && !tenantId.isEmpty();
    }
    
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
            
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        
        return false;
    }
    
    public Date getExpirationDateFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }
    
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromJwtToken(token);
        return expiration.before(new Date());
    }
    
    // Getter methods for expiration times
    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }
    
    public int getJwtRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }
}