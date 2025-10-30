package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for CompanyStoreUserRole
 */
public class CompanyStoreUserRoleResponse {
    
    private UUID id;
    private UUID membershipId;
    private CompanyRole role;
    private String roleDisplayName;
    private Boolean isActive;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private LocalDateTime revokedAt;
    private String revokedBy;
    
    // Constructors
    public CompanyStoreUserRoleResponse() {}
    
    public CompanyStoreUserRoleResponse(CompanyStoreUserRole roleMapping) {
        this.id = roleMapping.getId();
        this.membershipId = roleMapping.getCompanyStoreUser().getId();
        this.role = roleMapping.getRole();
        this.roleDisplayName = roleMapping.getRole().getDisplayName();
        this.isActive = roleMapping.getIsActive();
        this.assignedAt = roleMapping.getAssignedAt();
        this.assignedBy = roleMapping.getAssignedBy();
        this.revokedAt = roleMapping.getRevokedAt();
        this.revokedBy = roleMapping.getRevokedBy();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getMembershipId() {
        return membershipId;
    }
    
    public void setMembershipId(UUID membershipId) {
        this.membershipId = membershipId;
    }
    
    public CompanyRole getRole() {
        return role;
    }
    
    public void setRole(CompanyRole role) {
        this.role = role;
    }
    
    public String getRoleDisplayName() {
        return roleDisplayName;
    }
    
    public void setRoleDisplayName(String roleDisplayName) {
        this.roleDisplayName = roleDisplayName;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public String getRevokedBy() {
        return revokedBy;
    }
    
    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }
}
