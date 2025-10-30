package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for adding/removing individual roles to/from a user's membership
 */
public class RoleManagementRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Role is required")
    private CompanyRole role;
    
    // Constructors
    public RoleManagementRequest() {}
    
    public RoleManagementRequest(UUID userId, CompanyRole role) {
        this.userId = userId;
        this.role = role;
    }
    
    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public CompanyRole getRole() { return role; }
    public void setRole(CompanyRole role) { this.role = role; }
}
