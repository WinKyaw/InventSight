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
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateJwtToken(User user) {
        System.out.println("🔑 InventSight - Generating JWT token for user: " + user.getEmail());
        System.out.println("📅 Token generation time: 2025-08-26 09:04:35");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("system", "InventSight")
                .claim("createdBy", "WinKyaw")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
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
}