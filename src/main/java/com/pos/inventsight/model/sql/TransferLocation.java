package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transfer route representing a complete transfer path from one location to another.
 * Refactored in V39 to represent routes (from → to) instead of single locations.
 * 
 * A TransferLocation now encapsulates:
 * - Source location (from_id, from_location_type, from_name)
 * - Destination location (to_id, to_location_type, to_name)
 * 
 * This design:
 * - Simplifies transfer_requests (1 FK instead of 2)
 * - Enables route-level caching and reuse
 * - Denormalizes names for performance
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

    // === SOURCE LOCATION ===
    @NotNull(message = "Source location ID is required")
    @Column(name = "from_id", nullable = false, columnDefinition = "UUID")
    private UUID fromId;

    @NotNull(message = "Source location type is required")
    @Column(name = "from_location_type", nullable = false, length = 20)
    private String fromLocationType; // "WAREHOUSE", "STORE", or "MERCHANT"

    @Column(name = "from_name", length = 255)
    private String fromName; // Denormalized for quick access

    // === DESTINATION LOCATION ===
    @NotNull(message = "Destination location ID is required")
    @Column(name = "to_id", nullable = false, columnDefinition = "UUID")
    private UUID toId;

    @NotNull(message = "Destination location type is required")
    @Column(name = "to_location_type", nullable = false, length = 20)
    private String toLocationType; // "WAREHOUSE", "STORE", or "MERCHANT"

    @Column(name = "to_name", length = 255)
    private String toName; // Denormalized for quick access

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public TransferLocation() {
    }

    public TransferLocation(UUID fromId, String fromLocationType, String fromName,
                           UUID toId, String toLocationType, String toName) {
        this.fromId = fromId;
        this.fromLocationType = fromLocationType;
        this.fromName = fromName;
        this.toId = toId;
        this.toLocationType = toLocationType;
        this.toName = toName;
        this.createdAt = LocalDateTime.now();
    }

    // Static factory methods
    public static TransferLocation createRoute(UUID fromId, String fromLocationType, String fromName,
                                              UUID toId, String toLocationType, String toName) {
        return new TransferLocation(fromId, fromLocationType, fromName, toId, toLocationType, toName);
    }

    public static TransferLocation createRoute(Warehouse fromWarehouse, Store toStore) {
        return new TransferLocation(
            fromWarehouse.getId(), "WAREHOUSE", fromWarehouse.getName(),
            toStore.getId(), "STORE", toStore.getStoreName()
        );
    }

    public static TransferLocation createRoute(Store fromStore, Warehouse toWarehouse) {
        return new TransferLocation(
            fromStore.getId(), "STORE", fromStore.getStoreName(),
            toWarehouse.getId(), "WAREHOUSE", toWarehouse.getName()
        );
    }

    public static TransferLocation createRoute(Store fromStore, Store toStore) {
        return new TransferLocation(
            fromStore.getId(), "STORE", fromStore.getStoreName(),
            toStore.getId(), "STORE", toStore.getStoreName()
        );
    }

    public static TransferLocation createRoute(Warehouse fromWarehouse, Warehouse toWarehouse) {
        return new TransferLocation(
            fromWarehouse.getId(), "WAREHOUSE", fromWarehouse.getName(),
            toWarehouse.getId(), "WAREHOUSE", toWarehouse.getName()
        );
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFromId() {
        return fromId;
    }

    public void setFromId(UUID fromId) {
        this.fromId = fromId;
    }

    public String getFromLocationType() {
        return fromLocationType;
    }

    public void setFromLocationType(String fromLocationType) {
        this.fromLocationType = fromLocationType;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public UUID getToId() {
        return toId;
    }

    public void setToId(UUID toId) {
        this.toId = toId;
    }

    public String getToLocationType() {
        return toLocationType;
    }

    public void setToLocationType(String toLocationType) {
        this.toLocationType = toLocationType;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Validate that source and destination are different before persisting
     */
    @PrePersist
    @PreUpdate
    public void validate() {
        if (fromId == null || toId == null) {
            throw new IllegalStateException("TransferLocation must have both source and destination set");
        }
        
        if (fromId.equals(toId) && fromLocationType != null && fromLocationType.equals(toLocationType)) {
            throw new IllegalStateException("TransferLocation source and destination must be different");
        }
    }

    @Override
    public String toString() {
        return String.format("TransferLocation[%s:%s (%s) → %s:%s (%s)]",
            fromLocationType, fromId, fromName,
            toLocationType, toId, toName);
    }
}
