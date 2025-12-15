package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.UserNavigationPreference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user navigation preferences.
 */
public class NavigationPreferencesResponse {
    
    private UUID id;
    private UUID userId;
    private List<String> preferredTabs;
    private List<String> availableTabs;
    private LocalDateTime modifiedAt;
    
    // Constructors
    public NavigationPreferencesResponse() {}
    
    public NavigationPreferencesResponse(UserNavigationPreference preference) {
        this.id = preference.getId();
        this.userId = preference.getUserId();
        this.preferredTabs = preference.getPreferredTabs();
        this.availableTabs = preference.getAvailableTabs();
        this.modifiedAt = preference.getModifiedAt();
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
    
    public List<String> getPreferredTabs() {
        return preferredTabs;
    }
    
    public void setPreferredTabs(List<String> preferredTabs) {
        this.preferredTabs = preferredTabs;
    }
    
    public List<String> getAvailableTabs() {
        return availableTabs;
    }
    
    public void setAvailableTabs(List<String> availableTabs) {
        this.availableTabs = availableTabs;
    }
    
    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
