package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for warehouse inventory withdrawals
 */
public class WarehouseInventoryWithdrawalRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @Size(max = 200, message = "Destination must not exceed 200 characters")
    private String destination;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    private LocalDate withdrawalDate;

    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private WarehouseInventoryWithdrawal.TransactionType transactionType;

    // Getters and Setters
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

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

    public WarehouseInventoryWithdrawal.TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(WarehouseInventoryWithdrawal.TransactionType transactionType) { 
        this.transactionType = transactionType; 
    }
}