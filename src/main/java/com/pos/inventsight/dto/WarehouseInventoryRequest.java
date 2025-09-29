package com.pos.inventsight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating warehouse inventory levels
 */
public class WarehouseInventoryRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Current quantity is required")
    @Min(value = 0, message = "Current quantity cannot be negative")
    private Integer currentQuantity;

    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reservedQuantity = 0;

    @Min(value = 0, message = "Minimum stock level cannot be negative")
    private Integer minimumStockLevel = 0;

    @Min(value = 0, message = "Maximum stock level cannot be negative")
    private Integer maximumStockLevel;

    @Min(value = 0, message = "Reorder point cannot be negative")
    private Integer reorderPoint = 0;

    @Size(max = 100, message = "Location in warehouse must not exceed 100 characters")
    private String locationInWarehouse;

    // Getters and Setters
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(Integer currentQuantity) { this.currentQuantity = currentQuantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getMinimumStockLevel() { return minimumStockLevel; }
    public void setMinimumStockLevel(Integer minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; }

    public Integer getMaximumStockLevel() { return maximumStockLevel; }
    public void setMaximumStockLevel(Integer maximumStockLevel) { this.maximumStockLevel = maximumStockLevel; }

    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }

    public String getLocationInWarehouse() { return locationInWarehouse; }
    public void setLocationInWarehouse(String locationInWarehouse) { this.locationInWarehouse = locationInWarehouse; }
}