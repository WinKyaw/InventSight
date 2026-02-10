package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.StoreInventoryWithdrawal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for store inventory withdrawals
 */
public class StoreInventoryWithdrawalResponse {

    private UUID id;
    private UUID storeId;
    private String storeName;
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private String referenceNumber;
    private LocalDate withdrawalDate;
    private String reason;
    private String notes;
    private StoreInventoryWithdrawal.TransactionType transactionType;
    private StoreInventoryWithdrawal.TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public StoreInventoryWithdrawalResponse() {
    }

    public StoreInventoryWithdrawalResponse(StoreInventoryWithdrawal withdrawal) {
        this.id = withdrawal.getId();
        this.storeId = withdrawal.getStore() != null ? withdrawal.getStore().getId() : null;
        this.storeName = withdrawal.getStore() != null ? withdrawal.getStore().getStoreName() : null;
        this.productId = withdrawal.getProduct() != null ? withdrawal.getProduct().getId() : null;
        this.productName = withdrawal.getProduct() != null ? withdrawal.getProduct().getName() : null;
        this.productSku = withdrawal.getProduct() != null ? withdrawal.getProduct().getSku() : null;
        this.quantity = withdrawal.getQuantity();
        this.referenceNumber = withdrawal.getReferenceNumber();
        this.withdrawalDate = withdrawal.getWithdrawalDate();
        this.reason = withdrawal.getReason();
        this.notes = withdrawal.getNotes();
        this.transactionType = withdrawal.getTransactionType();
        this.status = withdrawal.getStatus();
        this.createdAt = withdrawal.getCreatedAt();
        this.updatedAt = withdrawal.getUpdatedAt();
        this.createdBy = withdrawal.getCreatedBy();
        this.updatedBy = withdrawal.getUpdatedBy();
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

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) { this.withdrawalDate = withdrawalDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public StoreInventoryWithdrawal.TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(StoreInventoryWithdrawal.TransactionType transactionType) { 
        this.transactionType = transactionType; 
    }

    public StoreInventoryWithdrawal.TransactionStatus getStatus() { return status; }
    public void setStatus(StoreInventoryWithdrawal.TransactionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
