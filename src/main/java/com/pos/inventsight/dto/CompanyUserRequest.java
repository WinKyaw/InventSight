package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for adding users to company or store
 */
public class CompanyUserRequest {
    
    @NotBlank(message = "Username or email is required")
    @Size(max = 100, message = "Username/email must not exceed 100 characters")
    private String usernameOrEmail;
    
    @NotNull(message = "Role is required")
    private CompanyRole role;
    
    private UUID storeId; // Optional - if null, user is added at company level
    
    // Constructors
    public CompanyUserRequest() {}
    
    public CompanyUserRequest(String usernameOrEmail, CompanyRole role) {
        this.usernameOrEmail = usernameOrEmail;
        this.role = role;
    }
    
    public CompanyUserRequest(String usernameOrEmail, CompanyRole role, UUID storeId) {
        this.usernameOrEmail = usernameOrEmail;
        this.role = role;
        this.storeId = storeId;
    }
    
    // Getters and Setters
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    
    public CompanyRole getRole() { return role; }
    public void setRole(CompanyRole role) { this.role = role; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
}