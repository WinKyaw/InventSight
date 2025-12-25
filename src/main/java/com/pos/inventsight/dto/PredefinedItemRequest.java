package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating predefined items
 */
public class PredefinedItemRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    @NotBlank(message = "Unit type is required")
    @Size(max = 50, message = "Unit type must not exceed 50 characters")
    private String unitType;
    
    private String description;
    
    private BigDecimal defaultPrice;
    
    // Constructors
    public PredefinedItemRequest() {}
    
    public PredefinedItemRequest(String name, String unitType) {
        this.name = name;
        this.unitType = unitType;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
}
