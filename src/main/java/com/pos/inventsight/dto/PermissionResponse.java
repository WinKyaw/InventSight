package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.PermissionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for one-time permissions
 */
public class PermissionResponse {
    
    private UUID id;
    private UUID grantedToUserId;
    private String grantedToUsername;
    private UUID grantedByUserId;
    private String grantedByUsername;
    private PermissionType permissionType;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Boolean isUsed;
    private Boolean isExpired;
    private Boolean isValid;
    private UUID storeId;
    
    // Constructors
    public PermissionResponse() {}
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getGrantedToUserId() {
        return grantedToUserId;
    }
    
    public void setGrantedToUserId(UUID grantedToUserId) {
        this.grantedToUserId = grantedToUserId;
    }
    
    public String getGrantedToUsername() {
        return grantedToUsername;
    }
    
    public void setGrantedToUsername(String grantedToUsername) {
        this.grantedToUsername = grantedToUsername;
    }
    
    public UUID getGrantedByUserId() {
        return grantedByUserId;
    }
    
    public void setGrantedByUserId(UUID grantedByUserId) {
        this.grantedByUserId = grantedByUserId;
    }
    
    public String getGrantedByUsername() {
        return grantedByUsername;
    }
    
    public void setGrantedByUsername(String grantedByUsername) {
        this.grantedByUsername = grantedByUsername;
    }
    
    public PermissionType getPermissionType() {
        return permissionType;
    }
    
    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }
    
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }
    
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public Boolean getIsUsed() {
        return isUsed;
    }
    
    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }
    
    public Boolean getIsExpired() {
        return isExpired;
    }
    
    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }
    
    public Boolean getIsValid() {
        return isValid;
    }
    
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
    
    public UUID getStoreId() {
        return storeId;
    }
    
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }
}
