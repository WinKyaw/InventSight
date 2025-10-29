package com.pos.inventsight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for adding an item to a sales order
 */
public class AddSalesOrderItemRequest {
    
    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;
    
    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @NotBlank(message = "Currency code is required")
    @Size(max = 3, min = 3, message = "Currency code must be exactly 3 characters")
    private String currencyCode;
    
    // Constructors
    public AddSalesOrderItemRequest() {
    }
    
    public AddSalesOrderItemRequest(UUID warehouseId, UUID productId, Integer quantity, 
                                    BigDecimal unitPrice, String currencyCode) {
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currencyCode = currencyCode;
    }
    
    // Getters and Setters
    public UUID getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
