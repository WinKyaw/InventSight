package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for updating user navigation preferences.
 */
public class NavigationPreferencesRequest {
    
    @NotNull(message = "Preferred tabs list cannot be null")
    private List<String> preferredTabs;
    
    // Constructors
    public NavigationPreferencesRequest() {}
    
    public NavigationPreferencesRequest(List<String> preferredTabs) {
        this.preferredTabs = preferredTabs;
    }
    
    // Getters and Setters
    public List<String> getPreferredTabs() {
        return preferredTabs;
    }
    
    public void setPreferredTabs(List<String> preferredTabs) {
        this.preferredTabs = preferredTabs;
    }
}
