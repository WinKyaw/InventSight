package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.PermissionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for granting one-time permissions
 */
public class GrantPermissionRequest {
    
    @NotNull
    private UUID grantedToUserId;
    
    @NotNull
    private PermissionType permissionType;
    
    private UUID storeId;
    
    // Constructors
    public GrantPermissionRequest() {}
    
    public GrantPermissionRequest(UUID grantedToUserId, PermissionType permissionType) {
        this.grantedToUserId = grantedToUserId;
        this.permissionType = permissionType;
    }
    
    // Getters and Setters
    public UUID getGrantedToUserId() {
        return grantedToUserId;
    }
    
    public void setGrantedToUserId(UUID grantedToUserId) {
        this.grantedToUserId = grantedToUserId;
    }
    
    public PermissionType getPermissionType() {
        return permissionType;
    }
    
    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }
    
    public UUID getStoreId() {
        return storeId;
    }
    
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }
}
