package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating/updating preexisting items
 */
public class PreexistingItemRequest {
    
    @NotNull
    private UUID storeId;
    
    @NotBlank
    @Size(max = 200)
    private String itemName;
    
    @Size(max = 100)
    private String category;
    
    private BigDecimal defaultPrice;
    
    @Size(max = 1000)
    private String description;
    
    @NotBlank
    @Size(max = 100)
    private String sku;
    
    // Constructors
    public PreexistingItemRequest() {}
    
    // Getters and Setters
    public UUID getStoreId() {
        return storeId;
    }
    
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }
    
    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
}
