package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding users to company with multiple roles
 */
public class CompanyUserMultiRoleRequest {
    
    @NotBlank(message = "Username or email is required")
    @Size(max = 100, message = "Username/email must not exceed 100 characters")
    private String usernameOrEmail;
    
    @NotEmpty(message = "At least one role is required")
    private List<CompanyRole> roles;
    
    private UUID storeId; // Optional - if null, user is added at company level
    
    // Constructors
    public CompanyUserMultiRoleRequest() {}
    
    public CompanyUserMultiRoleRequest(String usernameOrEmail, List<CompanyRole> roles) {
        this.usernameOrEmail = usernameOrEmail;
        this.roles = roles;
    }
    
    public CompanyUserMultiRoleRequest(String usernameOrEmail, List<CompanyRole> roles, UUID storeId) {
        this.usernameOrEmail = usernameOrEmail;
        this.roles = roles;
        this.storeId = storeId;
    }
    
    // Getters and Setters
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    
    public List<CompanyRole> getRoles() { return roles; }
    public void setRoles(List<CompanyRole> roles) { this.roles = roles; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
}
