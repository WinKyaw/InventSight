package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.UserPreferences;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user preferences information.
 */
public class UserPreferencesResponse {
    
    private UUID id;
    private UUID userId;
    private String preferredLanguage;
    private List<String> favoriteTabs = new ArrayList<>();
    private String theme;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public UserPreferencesResponse() {}
    
    public UserPreferencesResponse(UserPreferences preferences) {
        this.id = preferences.getId();
        this.userId = preferences.getUserId();
        this.preferredLanguage = preferences.getPreferredLanguage();
        this.favoriteTabs = preferences.getFavoriteTabs() != null ? 
                preferences.getFavoriteTabs() : new ArrayList<>();
        this.theme = preferences.getTheme();
        this.createdAt = preferences.getCreatedAt();
        this.updatedAt = preferences.getUpdatedAt();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public List<String> getFavoriteTabs() {
        return favoriteTabs;
    }
    
    public void setFavoriteTabs(List<String> favoriteTabs) {
        this.favoriteTabs = favoriteTabs;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
