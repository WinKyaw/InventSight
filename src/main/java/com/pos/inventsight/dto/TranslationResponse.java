package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Translation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for translation information.
 */
public class TranslationResponse {
    
    private UUID id;
    private String key;
    private String languageCode;
    private String value;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public TranslationResponse() {}
    
    public TranslationResponse(Translation translation) {
        this.id = translation.getId();
        this.key = translation.getKey();
        this.languageCode = translation.getLanguageCode();
        this.value = translation.getValue();
        this.category = translation.getCategory();
        this.createdAt = translation.getCreatedAt();
        this.updatedAt = translation.getUpdatedAt();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
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
