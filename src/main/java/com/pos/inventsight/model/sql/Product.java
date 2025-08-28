package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 200)
    private String name;
    
    @Size(max = 1000)
    private String description;
    
    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String sku;
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;
    
    @DecimalMin("0.0")
    private BigDecimal costPrice;

    @NotNull
    private Integer quantity;
    
    private Integer maxQuantity;
    
    @Size(max = 50)
    private String unit;
    
    @Size(max = 200)
    private String location;
    
    private LocalDate expiryDate;
    
    @Size(max = 100)
    private String category;
    
    @Size(max = 100)
    private String supplier;
    
    @Size(max = 50)
    private String barcode;
    
    private Integer lowStockThreshold;
    
    private Integer reorderLevel;
    
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Constructors
    public Product() {}
    
    public Product(String name, String sku, BigDecimal price, Integer quantity) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
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
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
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
    
    // Business Logic Methods
    public boolean isLowStock() {
        return lowStockThreshold != null && quantity <= lowStockThreshold;
    }
    
    public boolean isOutOfStock() {
        return quantity <= 0;
    }
    
    public boolean needsReorder() {
        return reorderLevel != null && quantity <= reorderLevel;
    }
    
    public BigDecimal getTotalValue() {
        return price.multiply(new BigDecimal(quantity));
    }
    
    public boolean isNearExpiry(int days) {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now().plusDays(days));
    }
    
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now());
    }
    
    public BigDecimal getProfitMargin() {
        if (costPrice == null || costPrice.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        return price.subtract(costPrice).divide(costPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
    }
}