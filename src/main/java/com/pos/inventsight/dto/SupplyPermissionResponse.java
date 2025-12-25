package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.SupplyManagementPermission;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for supply management permissions
 */
public class SupplyPermissionResponse {
    
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID companyId;
    private String companyName;
    private String permissionType;
    private Boolean isPermanent;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String grantedBy;
    private Boolean isActive;
    private String notes;
    
    // Constructors
    public SupplyPermissionResponse() {}
    
    public SupplyPermissionResponse(SupplyManagementPermission permission) {
        this.id = permission.getId();
        this.userId = permission.getUser() != null ? permission.getUser().getId() : null;
        this.userName = permission.getUser() != null ? permission.getUser().getUsername() : null;
        this.userEmail = permission.getUser() != null ? permission.getUser().getEmail() : null;
        this.companyId = permission.getCompany() != null ? permission.getCompany().getId() : null;
        this.companyName = permission.getCompany() != null ? permission.getCompany().getName() : null;
        this.permissionType = permission.getPermissionType();
        this.isPermanent = permission.getIsPermanent();
        this.grantedAt = permission.getGrantedAt();
        this.expiresAt = permission.getExpiresAt();
        this.grantedBy = permission.getGrantedBy() != null ? permission.getGrantedBy().getEmail() : null;
        this.isActive = permission.getIsActive();
        this.notes = permission.getNotes();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
    
    public Boolean getIsPermanent() { return isPermanent; }
    public void setIsPermanent(Boolean isPermanent) { this.isPermanent = isPermanent; }
    
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
