package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing user navigation preferences for role-based tab access control.
 * Prevents 403 errors by storing only tabs the user has permission to access.
 */
@Entity
@Table(name = "user_navigation_preferences")
public class UserNavigationPreference {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", type = org.hibernate.id.UUIDGenerator.class)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_tabs", columnDefinition = "jsonb")
    private List<String> preferredTabs = new ArrayList<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_tabs", columnDefinition = "jsonb")
    private List<String> availableTabs = new ArrayList<>();
    
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
    
    // Constructors
    public UserNavigationPreference() {
        this.preferredTabs = new ArrayList<>();
        this.availableTabs = new ArrayList<>();
    }
    
    public UserNavigationPreference(UUID userId) {
        this();
        this.userId = userId;
    }
    
    public UserNavigationPreference(UUID userId, List<String> preferredTabs, List<String> availableTabs) {
        this(userId);
        this.preferredTabs = preferredTabs != null ? preferredTabs : new ArrayList<>();
        this.availableTabs = availableTabs != null ? availableTabs : new ArrayList<>();
    }
    
    // JPA lifecycle callbacks
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public List<String> getPreferredTabs() {
        return preferredTabs;
    }
    
    public void setPreferredTabs(List<String> preferredTabs) {
        this.preferredTabs = preferredTabs;
    }
    
    public List<String> getAvailableTabs() {
        return availableTabs;
    }
    
    public void setAvailableTabs(List<String> availableTabs) {
        this.availableTabs = availableTabs;
    }
    
    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
