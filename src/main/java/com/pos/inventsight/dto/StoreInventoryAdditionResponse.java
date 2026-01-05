package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.StoreInventoryAddition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for store inventory additions (restocks)
 * Flattens product and store relationships for frontend consumption
 */
public class StoreInventoryAdditionResponse {
    
    private UUID id;
    private UUID storeId;
    private String storeName;
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String supplierName;
    private String referenceNumber;
    private LocalDate receiptDate;
    private LocalDate expiryDate;
    private String batchNumber;
    private String notes;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Constructor from entity
    public StoreInventoryAdditionResponse(StoreInventoryAddition addition) {
        this.id = addition.getId();
        this.storeId = addition.getStore() != null ? addition.getStore().getId() : null;
        this.storeName = addition.getStore() != null ? addition.getStore().getStoreName() : null;
        this.productId = addition.getProduct() != null ? addition.getProduct().getId() : null;
        this.productName = addition.getProduct() != null ? addition.getProduct().getName() : "Unknown Product";
        this.productSku = addition.getProduct() != null ? addition.getProduct().getSku() : null;
        this.quantity = addition.getQuantity();
        this.unitCost = addition.getUnitCost();
        this.totalCost = addition.getTotalCost();
        this.supplierName = addition.getSupplierName();
        this.referenceNumber = addition.getReferenceNumber();
        this.receiptDate = addition.getReceiptDate();
        this.expiryDate = addition.getExpiryDate();
        this.batchNumber = addition.getBatchNumber();
        this.notes = addition.getNotes();
        this.transactionType = addition.getTransactionType() != null ? addition.getTransactionType().name() : null;
        this.status = addition.getStatus() != null ? addition.getStatus().name() : null;
        this.createdAt = addition.getCreatedAt();
        this.updatedAt = addition.getUpdatedAt();
        this.createdBy = addition.getCreatedBy();
        this.updatedBy = addition.getUpdatedBy();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
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
    
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    
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
