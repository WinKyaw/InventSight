package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.PredefinedItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for predefined items
 */
public class PredefinedItemResponse {
    
    private UUID id;
    private String name;
    private String sku;
    private String category;
    private String unitType;
    private String description;
    private BigDecimal defaultPrice;
    private UUID companyId;
    private String companyName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    // Constructors
    public PredefinedItemResponse() {}
    
    public PredefinedItemResponse(PredefinedItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.sku = item.getSku();
        this.category = item.getCategory();
        this.unitType = item.getUnitType();
        this.description = item.getDescription();
        this.defaultPrice = item.getDefaultPrice();
        this.companyId = item.getCompany() != null ? item.getCompany().getId() : null;
        this.companyName = item.getCompany() != null ? item.getCompany().getName() : null;
        this.isActive = item.getIsActive();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
        this.createdBy = item.getCreatedByUser() != null ? item.getCreatedByUser().getEmail() : null;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
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
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
