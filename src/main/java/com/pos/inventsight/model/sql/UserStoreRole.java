package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_store_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "store_id"}))
public class UserStoreRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();
    
    @Column(name = "assigned_by")
    private String assignedBy;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoked_by")
    private String revokedBy;
    
    // Constructors
    public UserStoreRole() {}
    
    public UserStoreRole(User user, Store store, UserRole role) {
        this.user = user;
        this.store = store;
        this.role = role;
    }
    
    public UserStoreRole(User user, Store store, UserRole role, String assignedBy) {
        this.user = user;
        this.store = store;
        this.role = role;
        this.assignedBy = assignedBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
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
    
    // Business methods
    public void revokeRole(String revokedBy) {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
    }
    
    public void restoreRole() {
        this.isActive = true;
        this.revokedAt = null;
        this.revokedBy = null;
    }
    
    public boolean isOwnerOrCoOwner() {
        return role == UserRole.OWNER || role == UserRole.CO_OWNER;
    }
    
    public boolean isManager() {
        return role == UserRole.MANAGER;
    }
    
    public boolean hasAdminAccess() {
        return isOwnerOrCoOwner() || isManager();
    }
}