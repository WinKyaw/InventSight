package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for inventory availability
 */
public class InventoryAvailabilityResponse {
    
    private UUID productId;
    private String productName;
    private String productSku;
    private UUID warehouseId;
    private String warehouseName;
    private Integer available;
    private Integer reorderPoint;
    private BigDecimal price;
    private String currencyCode;
    
    // Constructors
    public InventoryAvailabilityResponse() {
    }
    
    public InventoryAvailabilityResponse(UUID productId, String productName, UUID warehouseId, 
                                        String warehouseName, Integer available, BigDecimal price, 
                                        String currencyCode) {
        this.productId = productId;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.available = available;
        this.price = price;
        this.currencyCode = currencyCode;
    }
    
    // Getters and Setters
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductSku() {
        return productSku;
    }
    
    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }
    
    public UUID getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public String getWarehouseName() {
        return warehouseName;
    }
    
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
    
    public Integer getAvailable() {
        return available;
    }
    
    public void setAvailable(Integer available) {
        this.available = available;
    }
    
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
