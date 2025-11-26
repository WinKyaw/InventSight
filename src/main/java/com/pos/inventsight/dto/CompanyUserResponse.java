package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for company user relationships
 */
public class CompanyUserResponse {
    
    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID storeId;
    private String storeName;
    private UUID userId;
    private String username;
    private String userFullName;
    private String userEmail;
    private CompanyRole role;
    private Boolean isActive;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private LocalDateTime revokedAt;
    private String revokedBy;
    
    // Constructors
    public CompanyUserResponse() {}
    
    public CompanyUserResponse(CompanyStoreUser companyStoreUser) {
        this.id = companyStoreUser.getId();
        this.companyId = companyStoreUser.getCompany().getId();
        this.companyName = companyStoreUser.getCompany().getName();
        
        if (companyStoreUser.getStore() != null) {
            this.storeId = companyStoreUser.getStore().getId();
            this.storeName = companyStoreUser.getStore().getStoreName();
        }
        
        this.userId = companyStoreUser.getUser().getId(); // Using UUID instead of ID
        this.username = companyStoreUser.getUser().getUsername();
        this.userFullName = companyStoreUser.getUser().getFullName();
        this.userEmail = companyStoreUser.getUser().getEmail();
        this.role = companyStoreUser.getRole();
        this.isActive = companyStoreUser.getIsActive();
        this.assignedAt = companyStoreUser.getAssignedAt();
        this.assignedBy = companyStoreUser.getAssignedBy();
        this.revokedAt = companyStoreUser.getRevokedAt();
        this.revokedBy = companyStoreUser.getRevokedBy();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public CompanyRole getRole() { return role; }
    public void setRole(CompanyRole role) { this.role = role; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    
    // Utility methods
    public boolean isCompanyLevel() {
        return storeId == null;
    }
    
    public boolean isStoreSpecific() {
        return storeId != null;
    }
    
    public String getRoleDisplayName() {
        return role.getDisplayName();
    }
}