package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating user preferences.
 */
public class UserPreferencesRequest {
    
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    private List<String> tabs;
    
    @Size(max = 20, message = "Theme must not exceed 20 characters")
    private String theme;
    
    // Constructors
    public UserPreferencesRequest() {}
    
    public UserPreferencesRequest(String languageCode) {
        this.languageCode = languageCode;
    }
    
    // Getters and Setters
    public String getLanguageCode() {
        return languageCode;
    }
    
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    
    public List<String> getTabs() {
        return tabs;
    }
    
    public void setTabs(List<String> tabs) {
        this.tabs = tabs;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
}
