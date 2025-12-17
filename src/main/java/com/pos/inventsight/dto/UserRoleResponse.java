package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for returning user's active role information.
 * This includes both company-specific roles and global user roles.
 */
public class UserRoleResponse {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private String roleSource; // "COMPANY_ROLE" or "GLOBAL_ROLE"
    private UUID companyId;
    private String companyName;
    private UUID storeId;
    private String storeName;
    private Boolean isTemporary;
    private Boolean isPermanent;
    private LocalDateTime expiresAt;
    private LocalDateTime assignedAt;
    private String assignedBy;
    
    // Constructors
    public UserRoleResponse() {}
    
    /**
     * Create response from company role
     */
    public static UserRoleResponse fromCompanyRole(User user, Company company, CompanyStoreUserRole role) {
        UserRoleResponse response = new UserRoleResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(role.getRole().name());
        response.setRoleSource("COMPANY_ROLE");
        response.setCompanyId(company.getId());
        response.setCompanyName(company.getName());
        response.setStoreId(role.getStore() != null ? role.getStore().getId() : null);
        response.setStoreName(role.getStore() != null ? role.getStore().getStoreName() : null);
        response.setIsTemporary(role.getExpiresAt() != null);
        response.setIsPermanent(role.getPermanent());
        response.setExpiresAt(role.getExpiresAt());
        response.setAssignedAt(role.getAssignedAt());
        response.setAssignedBy(role.getAssignedBy());
        return response;
    }
    
    /**
     * Create response from global user role (fallback)
     */
    public static UserRoleResponse fromGlobalRole(User user) {
        UserRoleResponse response = new UserRoleResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setRoleSource("GLOBAL_ROLE");
        response.setIsTemporary(false);
        response.setIsPermanent(true);
        return response;
    }
    
    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getRoleSource() {
        return roleSource;
    }
    
    public void setRoleSource(String roleSource) {
        this.roleSource = roleSource;
    }
    
    public UUID getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public UUID getStoreId() {
        return storeId;
    }
    
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }
    
    public String getStoreName() {
        return storeName;
    }
    
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    
    public Boolean getIsTemporary() {
        return isTemporary;
    }
    
    public void setIsTemporary(Boolean isTemporary) {
        this.isTemporary = isTemporary;
    }
    
    public Boolean getIsPermanent() {
        return isPermanent;
    }
    
    public void setIsPermanent(Boolean isPermanent) {
        this.isPermanent = isPermanent;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
}
