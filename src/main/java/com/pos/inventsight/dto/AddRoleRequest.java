package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a role to a membership
 */
public class AddRoleRequest {
    
    @NotNull(message = "Role is required")
    private CompanyRole role;
    
    // Constructors
    public AddRoleRequest() {}
    
    public AddRoleRequest(CompanyRole role) {
        this.role = role;
    }
    
    // Getters and Setters
    public CompanyRole getRole() {
        return role;
    }
    
    public void setRole(CompanyRole role) {
        this.role = role;
    }
}
