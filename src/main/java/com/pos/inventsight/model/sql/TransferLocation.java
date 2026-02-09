package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Polymorphic location reference for transfers.
 * Represents a location that can be a WAREHOUSE, STORE, or MERCHANT (future).
 * 
 * This design enforces that exactly ONE location type is set via database constraints
 * and entity-level validation.
 */
@Entity
@Table(name = "transfer_locations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransferLocation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @NotNull(message = "Location type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private LocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt",
        "createdBy",
        "updatedBy"
    })
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt",
        "createdBy",
        "updatedBy"
    })
    private Store store;

    @Column(name = "merchant_id", columnDefinition = "UUID")
    private UUID merchantId; // Future: will be FK to merchants table

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public TransferLocation() {
    }

    public TransferLocation(Warehouse warehouse) {
        this.locationType = LocationType.WAREHOUSE;
        this.warehouse = warehouse;
    }

    public TransferLocation(Store store) {
        this.locationType = LocationType.STORE;
        this.store = store;
    }

    // Static factory methods
    public static TransferLocation forWarehouse(Warehouse warehouse) {
        return new TransferLocation(warehouse);
    }

    public static TransferLocation forStore(Store store) {
        return new TransferLocation(store);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        if (warehouse != null) {
            this.locationType = LocationType.WAREHOUSE;
            this.store = null;
            this.merchantId = null;
        }
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
        if (store != null) {
            this.locationType = LocationType.STORE;
            this.warehouse = null;
            this.merchantId = null;
        }
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
        if (merchantId != null) {
            this.locationType = LocationType.MERCHANT;
            this.warehouse = null;
            this.store = null;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public UUID getLocationId() {
        switch (locationType) {
            case WAREHOUSE:
                return warehouse != null ? warehouse.getId() : null;
            case STORE:
                return store != null ? store.getId() : null;
            case MERCHANT:
                return merchantId;
            default:
                return null;
        }
    }

    public String getLocationName() {
        switch (locationType) {
            case WAREHOUSE:
                return warehouse != null ? warehouse.getName() : "Unknown Warehouse";
            case STORE:
                return store != null ? store.getStoreName() : "Unknown Store";
            case MERCHANT:
                return "Merchant #" + merchantId;
            default:
                return "Unknown Location";
        }
    }

    /**
     * Validate that exactly ONE location is set before persisting
     */
    @PrePersist
    @PreUpdate
    public void validate() {
        int count = 0;
        if (warehouse != null) count++;
        if (store != null) count++;
        if (merchantId != null) count++;
        
        if (count != 1) {
            throw new IllegalStateException("TransferLocation must have exactly ONE location set");
        }
    }

    // Enum for location types
    public enum LocationType {
        WAREHOUSE,
        STORE,
        MERCHANT
    }
}
