package com.pos.inventsight.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidationService {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    
    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "123456", "password123", "admin", "qwerty", "letmein",
        "welcome", "monkey", "dragon", "password1", "123456789", "football"
    );
    
    public PasswordValidationResult validatePassword(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.trim().isEmpty()) {
            errors.add("Password is required");
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }
        
        // Length check
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }
        
        // Character requirements
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\\\,.<>/?)");
        }
        
        // Common password check
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common. Please choose a more secure password");
        }
        
        // Sequential characters check
        if (hasSequentialCharacters(password)) {
            errors.add("Password should not contain sequential characters (like 123 or abc)");
        }
        
        // Calculate strength
        int strengthScore = calculateStrengthScore(password);
        result.setStrengthScore(strengthScore);
        result.setStrength(getStrengthLevel(strengthScore));
        
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        
        return result;
    }
    
    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            // Check for ascending sequence
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
            
            // Check for descending sequence  
            if (c2 == c1 - 1 && c3 == c2 - 1) {
                return true;
            }
        }
        return false;
    }
    
    private int calculateStrengthScore(String password) {
        int score = 0;
        
        // Length bonus
        score += Math.min(password.length() * 2, 20);
        
        // Character variety bonus
        if (LOWERCASE_PATTERN.matcher(password).matches()) score += 5;
        if (UPPERCASE_PATTERN.matcher(password).matches()) score += 5;
        if (DIGIT_PATTERN.matcher(password).matches()) score += 5;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score += 10;
        
        // Unique characters bonus
        long uniqueChars = password.chars().distinct().count();
        score += (int) Math.min(uniqueChars * 2, 20);
        
        return Math.min(score, 100);
    }
    
    private String getStrengthLevel(int score) {
        if (score < 30) return "Weak";
        if (score < 60) return "Fair";
        if (score < 80) return "Good";
        return "Strong";
    }
    
    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private int strengthScore;
        private String strength;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public int getStrengthScore() { return strengthScore; }
        public void setStrengthScore(int strengthScore) { this.strengthScore = strengthScore; }
        
        public String getStrength() { return strength; }
        public void setStrength(String strength) { this.strength = strength; }
    }
}