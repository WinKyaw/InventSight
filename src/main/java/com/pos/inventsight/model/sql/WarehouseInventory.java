package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WarehouseInventory entity representing current inventory levels per warehouse per product
 */
@Entity
@Table(name = "warehouse_inventory",
       uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id"}))
public class WarehouseInventory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @NotNull(message = "Warehouse is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Current quantity is required")
    @Min(value = 0, message = "Current quantity cannot be negative")
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity = 0;

    @NotNull(message = "Reserved quantity is required")
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    // Available quantity is computed column in database (current - reserved)
    // We include it here for JPA mapping but it's read-only
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private Integer availableQuantity;

    @Min(value = 0, message = "Minimum stock level cannot be negative")
    @Column(name = "minimum_stock_level")
    private Integer minimumStockLevel = 0;

    @Min(value = 0, message = "Maximum stock level cannot be negative")
    @Column(name = "maximum_stock_level")
    private Integer maximumStockLevel;

    @Min(value = 0, message = "Reorder point cannot be negative")
    @Column(name = "reorder_point")
    private Integer reorderPoint = 0;

    @Size(max = 100, message = "Location in warehouse must not exceed 100 characters")
    @Column(name = "location_in_warehouse")
    private String locationInWarehouse;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Constructors
    public WarehouseInventory() {
        // Default constructor
    }

    public WarehouseInventory(Warehouse warehouse, Product product, Integer currentQuantity) {
        this();
        this.warehouse = warehouse;
        this.product = product;
        this.currentQuantity = currentQuantity;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(Integer currentQuantity) { 
        this.currentQuantity = currentQuantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { 
        this.reservedQuantity = reservedQuantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public Integer getAvailableQuantity() { 
        // Fallback calculation if database computed column not available
        return availableQuantity != null ? availableQuantity : (currentQuantity - reservedQuantity);
    }

    public Integer getMinimumStockLevel() { return minimumStockLevel; }
    public void setMinimumStockLevel(Integer minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; }

    public Integer getMaximumStockLevel() { return maximumStockLevel; }
    public void setMaximumStockLevel(Integer maximumStockLevel) { this.maximumStockLevel = maximumStockLevel; }

    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }

    public String getLocationInWarehouse() { return locationInWarehouse; }
    public void setLocationInWarehouse(String locationInWarehouse) { this.locationInWarehouse = locationInWarehouse; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Business methods
    public boolean isLowStock() {
        return getAvailableQuantity() <= reorderPoint;
    }

    public boolean isOverstock() {
        return maximumStockLevel != null && currentQuantity > maximumStockLevel;
    }

    public boolean canReserve(Integer quantity) {
        return quantity != null && quantity > 0 && getAvailableQuantity() >= quantity;
    }

    public void reserveQuantity(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException("Cannot reserve " + quantity + " units. Available: " + getAvailableQuantity());
        }
        this.reservedQuantity += quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public void releaseReservation(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be positive");
        }
        if (quantity > reservedQuantity) {
            throw new IllegalArgumentException("Cannot release " + quantity + " units. Reserved: " + reservedQuantity);
        }
        this.reservedQuantity -= quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public void addStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Add stock quantity must be positive");
        }
        this.currentQuantity += quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public void removeStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Remove stock quantity must be positive");
        }
        if (quantity > getAvailableQuantity()) {
            throw new IllegalArgumentException("Cannot remove " + quantity + " units. Available: " + getAvailableQuantity());
        }
        this.currentQuantity -= quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
}