package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking changes for offline sync
 * Provides change feed for incremental synchronization
 */
@Entity
@Table(name = "sync_changes")
public class SyncChange {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "company_id")
    private UUID companyId;
    
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;
    
    @Column(name = "operation", nullable = false, length = 20)
    private String operation; // INSERT, UPDATE, DELETE
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
    
    @Column(name = "change_data", columnDefinition = "TEXT")
    private String changeData;
    
    @Version
    private Long version;
    
    public SyncChange() {
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public UUID getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public String getChangeData() {
        return changeData;
    }
    
    public void setChangeData(String changeData) {
        this.changeData = changeData;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}
