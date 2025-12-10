package com.pos.inventsight.dto;

import java.util.List;

/**
 * Response DTO for available languages.
 */
public class LanguageResponse {
    
    private List<String> languages;
    private String defaultLanguage;
    
    // Constructors
    public LanguageResponse() {}
    
    public LanguageResponse(List<String> languages, String defaultLanguage) {
        this.languages = languages;
        this.defaultLanguage = defaultLanguage;
    }
    
    // Getters and Setters
    public List<String> getLanguages() {
        return languages;
    }
    
    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
