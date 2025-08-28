package com.pos.inventsight.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    
    // In-memory rate limiting (in production, use Redis)
    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();
    
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int MAX_EMAIL_VERIFICATION_ATTEMPTS = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 60;
    
    public boolean isRegistrationAllowed(String ipAddress, String email) {
        String key = "registration:" + ipAddress + ":" + email;
        return checkRateLimit(key, MAX_REGISTRATION_ATTEMPTS);
    }
    
    public boolean isEmailVerificationAllowed(String ipAddress, String email) {
        String key = "email-verification:" + ipAddress + ":" + email;
        return checkRateLimit(key, MAX_EMAIL_VERIFICATION_ATTEMPTS);
    }
    
    public void recordRegistrationAttempt(String ipAddress, String email) {
        String key = "registration:" + ipAddress + ":" + email;
        recordAttempt(key);
    }
    
    public void recordEmailVerificationAttempt(String ipAddress, String email) {
        String key = "email-verification:" + ipAddress + ":" + email;
        recordAttempt(key);
    }
    
    public RateLimitStatus getRateLimitStatus(String ipAddress, String email, String operation) {
        String key = operation + ":" + ipAddress + ":" + email;
        RateLimitEntry entry = attempts.get(key);
        
        if (entry == null) {
            return new RateLimitStatus(true, 0, getMaxAttempts(operation));
        }
        
        // Clean up expired entries
        if (isExpired(entry)) {
            attempts.remove(key);
            return new RateLimitStatus(true, 0, getMaxAttempts(operation));
        }
        
        int maxAttempts = getMaxAttempts(operation);
        return new RateLimitStatus(
            entry.getAttempts() < maxAttempts,
            entry.getAttempts(),
            maxAttempts,
            entry.getLastAttempt().plusMinutes(RATE_LIMIT_WINDOW_MINUTES)
        );
    }
    
    private boolean checkRateLimit(String key, int maxAttempts) {
        RateLimitEntry entry = attempts.get(key);
        
        if (entry == null) {
            return true;
        }
        
        // Clean up expired entries
        if (isExpired(entry)) {
            attempts.remove(key);
            return true;
        }
        
        return entry.getAttempts() < maxAttempts;
    }
    
    private void recordAttempt(String key) {
        RateLimitEntry entry = attempts.get(key);
        LocalDateTime now = LocalDateTime.now();
        
        if (entry == null || isExpired(entry)) {
            attempts.put(key, new RateLimitEntry(1, now));
        } else {
            entry.setAttempts(entry.getAttempts() + 1);
            entry.setLastAttempt(now);
        }
    }
    
    private boolean isExpired(RateLimitEntry entry) {
        return entry.getLastAttempt().isBefore(LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES));
    }
    
    private int getMaxAttempts(String operation) {
        switch (operation) {
            case "registration": return MAX_REGISTRATION_ATTEMPTS;
            case "email-verification": return MAX_EMAIL_VERIFICATION_ATTEMPTS;
            default: return 5;
        }
    }
    
    // Clean up expired entries (should be called periodically)
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        attempts.entrySet().removeIf(entry -> entry.getValue().getLastAttempt().isBefore(cutoff));
    }
    
    private static class RateLimitEntry {
        private int attempts;
        private LocalDateTime lastAttempt;
        
        public RateLimitEntry(int attempts, LocalDateTime lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }
        
        // Getters and setters
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        
        public LocalDateTime getLastAttempt() { return lastAttempt; }
        public void setLastAttempt(LocalDateTime lastAttempt) { this.lastAttempt = lastAttempt; }
    }
    
    public static class RateLimitStatus {
        private boolean allowed;
        private int attempts;
        private int maxAttempts;
        private LocalDateTime resetTime;
        
        public RateLimitStatus(boolean allowed, int attempts, int maxAttempts) {
            this.allowed = allowed;
            this.attempts = attempts;
            this.maxAttempts = maxAttempts;
        }
        
        public RateLimitStatus(boolean allowed, int attempts, int maxAttempts, LocalDateTime resetTime) {
            this.allowed = allowed;
            this.attempts = attempts;
            this.maxAttempts = maxAttempts;
            this.resetTime = resetTime;
        }
        
        // Getters
        public boolean isAllowed() { return allowed; }
        public int getAttempts() { return attempts; }
        public int getMaxAttempts() { return maxAttempts; }
        public LocalDateTime getResetTime() { return resetTime; }
    }
}