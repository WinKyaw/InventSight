package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WarehouseInventoryWithdrawal entity representing inventory withdrawals/issues
 */
@Entity
@Table(name = "warehouse_inventory_withdrawals")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WarehouseInventoryWithdrawal {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @NotNull(message = "Warehouse is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse warehouse;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @DecimalMin(value = "0.0", message = "Total cost cannot be negative")
    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Size(max = 200, message = "Destination must not exceed 200 characters")
    private String destination;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate = LocalDate.now();

    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50)
    private TransactionType transactionType = TransactionType.ISSUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Constructors
    public WarehouseInventoryWithdrawal() {
        // Default constructor
    }

    public WarehouseInventoryWithdrawal(Warehouse warehouse, Product product, Integer quantity, String reason) {
        this();
        this.warehouse = warehouse;
        this.product = product;
        this.quantity = quantity;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { 
        this.unitCost = unitCost;
        calculateTotalCost();
    }

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

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Business methods
    public void calculateTotalCost() {
        if (unitCost != null && quantity != null) {
            this.totalCost = unitCost.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @PrePersist
    public void prePersist() {
        calculateTotalCost();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotalCost();
    }

    // Enums
    public enum TransactionType {
        ISSUE("Issue"),
        TRANSFER_OUT("Transfer Out"),
        ADJUSTMENT_OUT("Adjustment Out"),
        DAMAGE("Damage"),
        EXPIRED("Expired"),
        THEFT("Theft");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransactionStatus {
        PENDING("Pending"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        private final String displayName;

        TransactionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}