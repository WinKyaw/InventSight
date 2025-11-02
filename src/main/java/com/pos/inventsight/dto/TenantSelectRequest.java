package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TenantSelectRequest {
    
    @NotBlank(message = "Tenant ID is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "Tenant ID must be a valid UUID format")
    private String tenantId;
    
    // Constructors
    public TenantSelectRequest() {}
    
    public TenantSelectRequest(String tenantId) {
        this.tenantId = tenantId;
    }
    
    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    @Override
    public String toString() {
        return "TenantSelectRequest{" +
                "tenantId='" + tenantId + '\'' +
                '}';
    }
}
