package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO for creating a transfer request
 */
public class CreateTransferRequestDTO {
    
    @NotBlank(message = "From location type is required")
    private String fromLocationType; // "WAREHOUSE" or "STORE"
    
    @NotNull(message = "From location ID is required")
    private UUID fromLocationId;
    
    @NotBlank(message = "To location type is required")
    private String toLocationType; // "WAREHOUSE" or "STORE"
    
    @NotNull(message = "To location ID is required")
    private UUID toLocationId;
    
    private String itemName;
    
    private String itemSku;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1")
    private Integer requestedQuantity;
    
    private String priority; // HIGH, MEDIUM, LOW
    
    private String reason;
    
    private String notes;
    
    // Constructors
    public CreateTransferRequestDTO() {
    }
    
    // Getters and Setters
    public String getFromLocationType() {
        return fromLocationType;
    }
    
    public void setFromLocationType(String fromLocationType) {
        this.fromLocationType = fromLocationType;
    }
    
    public UUID getFromLocationId() {
        return fromLocationId;
    }
    
    public void setFromLocationId(UUID fromLocationId) {
        this.fromLocationId = fromLocationId;
    }
    
    public String getToLocationType() {
        return toLocationType;
    }
    
    public void setToLocationType(String toLocationType) {
        this.toLocationType = toLocationType;
    }
    
    public UUID getToLocationId() {
        return toLocationId;
    }
    
    public void setToLocationId(UUID toLocationId) {
        this.toLocationId = toLocationId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemSku() {
        return itemSku;
    }
    
    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
