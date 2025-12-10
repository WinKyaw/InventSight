package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating translations.
 */
public class TranslationRequest {
    
    @NotBlank(message = "Translation key is required")
    @Size(max = 200, message = "Key must not exceed 200 characters")
    private String key;
    
    @NotBlank(message = "Language code is required")
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    @NotBlank(message = "Translation value is required")
    private String value;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    // Constructors
    public TranslationRequest() {}
    
    public TranslationRequest(String key, String languageCode, String value, String category) {
        this.key = key;
        this.languageCode = languageCode;
        this.value = value;
        this.category = category;
    }
    
    // Getters and Setters
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getLanguageCode() {
        return languageCode;
    }
    
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}
