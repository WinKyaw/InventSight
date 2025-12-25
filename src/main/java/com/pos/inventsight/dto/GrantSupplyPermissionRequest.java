package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for granting supply management permission
 */
public class GrantSupplyPermissionRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Company ID is required")
    private UUID companyId;
    
    private Boolean isPermanent = true;
    
    private LocalDateTime expiresAt;
    
    private String notes;
    
    // Constructors
    public GrantSupplyPermissionRequest() {}
    
    public GrantSupplyPermissionRequest(UUID userId, UUID companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }
    
    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public Boolean getIsPermanent() { return isPermanent; }
    public void setIsPermanent(Boolean isPermanent) { this.isPermanent = isPermanent; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
