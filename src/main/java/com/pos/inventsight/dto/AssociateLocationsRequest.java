package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for associating stores or warehouses with predefined items
 */
public class AssociateLocationsRequest {
    
    @NotNull(message = "Location IDs are required")
    private List<UUID> locationIds;
    
    // Constructors
    public AssociateLocationsRequest() {}
    
    public AssociateLocationsRequest(List<UUID> locationIds) {
        this.locationIds = locationIds;
    }
    
    // Getters and Setters
    public List<UUID> getLocationIds() { return locationIds; }
    public void setLocationIds(List<UUID> locationIds) { this.locationIds = locationIds; }
}
