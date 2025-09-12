package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductResponse {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String sku;
    private String category;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer quantity;
    private Integer maxQuantity;
    private String unit;
    private String supplier;
    private String location;
    private String barcode;
    private LocalDate expiryDate;
    private Integer lowStockThreshold;
    private Integer reorderLevel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Business logic fields
    private BigDecimal totalValue;
    private boolean isLowStock;
    private boolean isOutOfStock;
    private boolean needsReorder;
    private boolean isExpired;
    private boolean isNearExpiry;
    private BigDecimal profitMargin;

    public ProductResponse() {}

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.uuid = product.getUuid(); // Product.getUuid() returns String for external compatibility
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.category = product.getCategory();
        this.price = product.getPrice();
        this.costPrice = product.getCostPrice();
        this.quantity = product.getQuantity();
        this.maxQuantity = product.getMaxQuantity();
        this.unit = product.getUnit();
        this.supplier = product.getSupplier();
        this.location = product.getLocation();
        this.barcode = product.getBarcode();
        this.expiryDate = product.getExpiryDate();
        this.lowStockThreshold = product.getLowStockThreshold();
        this.reorderLevel = product.getReorderLevel();
        this.isActive = product.getIsActive();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
        this.createdBy = product.getCreatedBy();
        this.updatedBy = product.getUpdatedBy();
        
        // Calculate business logic fields
        this.totalValue = product.getTotalValue();
        this.isLowStock = product.isLowStock();
        this.isOutOfStock = product.isOutOfStock();
        this.needsReorder = product.needsReorder();
        this.isExpired = product.isExpired();
        this.isNearExpiry = product.isNearExpiry(30); // 30 days warning
        this.profitMargin = product.getProfitMargin();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public boolean isLowStock() { return isLowStock; }
    public void setLowStock(boolean lowStock) { isLowStock = lowStock; }

    public boolean isOutOfStock() { return isOutOfStock; }
    public void setOutOfStock(boolean outOfStock) { isOutOfStock = outOfStock; }

    public boolean isNeedsReorder() { return needsReorder; }
    public void setNeedsReorder(boolean needsReorder) { this.needsReorder = needsReorder; }

    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }

    public boolean isNearExpiry() { return isNearExpiry; }
    public void setNearExpiry(boolean nearExpiry) { isNearExpiry = nearExpiry; }

    public BigDecimal getProfitMargin() { return profitMargin; }
    public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }
}