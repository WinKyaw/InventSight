package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit event entity for tamper-evident audit trail.
 * Records must not be updated or deleted after insertion.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt = LocalDateTime.now();
    
    @NotBlank
    @Column(nullable = false)
    private String actor;
    
    @Column(name = "actor_id")
    private Long actorId;
    
    @NotBlank
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(name = "entity_type", length = 100)
    private String entityType;
    
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    @Column(name = "tenant_id", columnDefinition = "UUID")
    private UUID tenantId;
    
    @Column(name = "company_id", columnDefinition = "UUID")
    private UUID companyId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;
    
    @Column(name = "prev_hash", length = 64)
    private String prevHash;
    
    @Column(length = 64)
    private String hash;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public AuditEvent() {}
    
    public AuditEvent(String actor, Long actorId, String action) {
        this.actor = actor;
        this.actorId = actorId;
        this.action = action;
    }
    
    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters (no setters for immutability after persistence)
    public UUID getId() { return id; }
    public LocalDateTime getEventAt() { return eventAt; }
    public String getActor() { return actor; }
    public Long getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCompanyId() { return companyId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getDetailsJson() { return detailsJson; }
    public String getPrevHash() { return prevHash; }
    public String getHash() { return hash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Setters (public for AuditService but should only be used during construction)
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
    public void setActor(String actor) { this.actor = actor; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public void setAction(String action) { this.action = action; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public void setHash(String hash) { this.hash = hash; }
    
    public static class Builder {
        private final AuditEvent event = new AuditEvent();
        
        public Builder actor(String actor) {
            event.setActor(actor);
            return this;
        }
        
        public Builder actorId(Long actorId) {
            event.setActorId(actorId);
            return this;
        }
        
        public Builder action(String action) {
            event.setAction(action);
            return this;
        }
        
        public Builder entityType(String entityType) {
            event.setEntityType(entityType);
            return this;
        }
        
        public Builder entityId(String entityId) {
            event.setEntityId(entityId);
            return this;
        }
        
        public Builder tenantId(UUID tenantId) {
            event.setTenantId(tenantId);
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            event.setCompanyId(companyId);
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            event.setIpAddress(ipAddress);
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            event.setUserAgent(userAgent);
            return this;
        }
        
        public Builder detailsJson(String detailsJson) {
            event.setDetailsJson(detailsJson);
            return this;
        }
        
        public Builder prevHash(String prevHash) {
            event.setPrevHash(prevHash);
            return this;
        }
        
        public Builder hash(String hash) {
            event.setHash(hash);
            return this;
        }
        
        public AuditEvent build() {
            return event;
        }
    }
}
