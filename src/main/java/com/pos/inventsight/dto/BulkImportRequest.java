package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for bulk importing translations.
 */
public class BulkImportRequest {
    
    @NotBlank(message = "Language code is required")
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    @NotNull(message = "Translations map is required")
    private Map<String, String> translations;
    
    // Constructors
    public BulkImportRequest() {}
    
    public BulkImportRequest(String languageCode, Map<String, String> translations) {
        this.languageCode = languageCode;
        this.translations = translations;
    }
    
    // Getters and Setters
    public String getLanguageCode() {
        return languageCode;
    }
    
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    
    public Map<String, String> getTranslations() {
        return translations;
    }
    
    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }
}
