package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.StoreInventoryAddition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for store inventory additions
 */
public class StoreInventoryAdditionRequest {

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @Size(max = 200, message = "Supplier name must not exceed 200 characters")
    private String supplierName;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    private LocalDate receiptDate;

    private LocalDate expiryDate;

    @Size(max = 100, message = "Batch number must not exceed 100 characters")
    private String batchNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private StoreInventoryAddition.TransactionType transactionType;

    // Getters and Setters
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

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

    public StoreInventoryAddition.TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(StoreInventoryAddition.TransactionType transactionType) { 
        this.transactionType = transactionType; 
    }
}
