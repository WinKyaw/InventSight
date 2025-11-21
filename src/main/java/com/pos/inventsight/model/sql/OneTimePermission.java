package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OneTimePermission entity for temporary permission grants to employees.
 * Permissions expire after 1 use OR 1 hour, whichever comes first.
 */
@Entity
@Table(name = "one_time_permissions")
public class OneTimePermission {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_to_user_id", nullable = false)
    private User grantedToUser;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id", nullable = false)
    private User grantedByUser;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false)
    private PermissionType permissionType;
    
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired = false;
    
    // Optional: Store reference for store-scoped permissions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;
    
    // Constructors
    public OneTimePermission() {}
    
    public OneTimePermission(User grantedToUser, User grantedByUser, 
                           PermissionType permissionType, LocalDateTime expiresAt) {
        this.grantedToUser = grantedToUser;
        this.grantedByUser = grantedByUser;
        this.permissionType = permissionType;
        this.expiresAt = expiresAt;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public User getGrantedToUser() {
        return grantedToUser;
    }
    
    public void setGrantedToUser(User grantedToUser) {
        this.grantedToUser = grantedToUser;
    }
    
    public User getGrantedByUser() {
        return grantedByUser;
    }
    
    public void setGrantedByUser(User grantedByUser) {
        this.grantedByUser = grantedByUser;
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
    
    public Store getStore() {
        return store;
    }
    
    public void setStore(Store store) {
        this.store = store;
    }
    
    // Business methods
    public boolean isValid() {
        return !isUsed && !isExpired && LocalDateTime.now().isBefore(expiresAt);
    }
    
    public void consume() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }
    
    public void markAsExpired() {
        this.isExpired = true;
    }
    
    public boolean shouldBeExpired() {
        return !isUsed && !isExpired && LocalDateTime.now().isAfter(expiresAt);
    }
}
