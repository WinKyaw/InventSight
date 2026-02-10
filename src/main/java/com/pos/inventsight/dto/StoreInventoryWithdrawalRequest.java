package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.StoreInventoryWithdrawal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for store inventory withdrawals
 */
public class StoreInventoryWithdrawalRequest {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    private LocalDate withdrawalDate;

    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private StoreInventoryWithdrawal.TransactionType transactionType;

    // Getters and Setters
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

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
}
