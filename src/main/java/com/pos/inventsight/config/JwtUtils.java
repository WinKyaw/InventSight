package com.pos.inventsight.config;

import com.pos.inventsight.model.sql.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {
    
    @Value("${inventsight.security.jwt.secret:inventsight-super-secret-jwt-key-change-in-production-2025}")
    private String jwtSecret;
    
    @Value("${inventsight.security.jwt.expiration:86400000}")
    private int jwtExpirationMs;
    
    @Value("${inventsight.security.jwt.refresh-expiration:604800000}")
    private int jwtRefreshExpirationMs; // 7 days
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateJwtToken(User user) {
        return generateJwtToken(user, null);
    }
    
    /**
     * Generate JWT token with optional tenant_id claim
     */
    public String generateJwtToken(User user, String tenantId) {
        System.out.println("🔑 InventSight - Generating JWT token for user: " + user.getEmail());
        System.out.println("📅 Token generation time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("system", "InventSight")
                .claim("createdBy", "WinKyaw")
                .claim("tokenType", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs));
        
        // Add tenant_id if provided
        if (tenantId != null && !tenantId.isEmpty()) {
            builder.claim("tenant_id", tenantId);
        }
        
        return builder.signWith(getSigningKey()).compact();
    }
    
    public String generateRefreshToken(User user) {
        System.out.println("🔄 InventSight - Generating refresh token for user: " + user.getEmail());
        
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
            System.out.println("❌ InventSight Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("❌ InventSight JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ InventSight JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ InventSight JWT claims string is empty: " + e.getMessage());
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