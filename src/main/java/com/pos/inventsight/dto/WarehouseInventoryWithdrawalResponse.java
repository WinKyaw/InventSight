package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for warehouse inventory withdrawals (sales)
 * Flattens product and warehouse relationships for frontend consumption
 */
public class WarehouseInventoryWithdrawalResponse {
    
    private UUID id;
    private UUID warehouseId;
    private String warehouseName;
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String destination;
    private String referenceNumber;
    private LocalDate withdrawalDate;
    private String reason;
    private String notes;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Constructor from entity
    public WarehouseInventoryWithdrawalResponse(WarehouseInventoryWithdrawal withdrawal) {
        this.id = withdrawal.getId();
        this.warehouseId = withdrawal.getWarehouse() != null ? withdrawal.getWarehouse().getId() : null;
        this.warehouseName = withdrawal.getWarehouse() != null ? withdrawal.getWarehouse().getName() : null;
        this.productId = withdrawal.getProduct() != null ? withdrawal.getProduct().getId() : null;
        this.productName = withdrawal.getProduct() != null ? withdrawal.getProduct().getName() : "Unknown Product";
        this.productSku = withdrawal.getProduct() != null ? withdrawal.getProduct().getSku() : null;
        this.quantity = withdrawal.getQuantity();
        this.unitCost = withdrawal.getUnitCost();
        this.totalCost = withdrawal.getTotalCost();
        this.destination = withdrawal.getDestination();
        this.referenceNumber = withdrawal.getReferenceNumber();
        this.withdrawalDate = withdrawal.getWithdrawalDate();
        this.reason = withdrawal.getReason();
        this.notes = withdrawal.getNotes();
        this.transactionType = withdrawal.getTransactionType() != null ? withdrawal.getTransactionType().name() : null;
        this.status = withdrawal.getStatus() != null ? withdrawal.getStatus().name() : null;
        this.createdAt = withdrawal.getCreatedAt();
        this.updatedAt = withdrawal.getUpdatedAt();
        this.createdBy = withdrawal.getCreatedBy();
        this.updatedBy = withdrawal.getUpdatedBy();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    
    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) { this.withdrawalDate = withdrawalDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
