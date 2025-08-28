package com.pos.inventsight.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ActivityResponse {
    
    private String id;
    private String userId;
    private String username;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String module;
    private String severity;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    
    // Constructors
    public ActivityResponse() {}
    
    public ActivityResponse(String id, String userId, String username, String action, 
                          String entityType, String entityId, String description,
                          String module, String severity, LocalDateTime timestamp,
                          Map<String, Object> metadata, Map<String, Object> beforeData, 
                          Map<String, Object> afterData) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.module = module;
        this.severity = severity;
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.beforeData = beforeData;
        this.afterData = afterData;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, Object> getBeforeData() { return beforeData; }
    public void setBeforeData(Map<String, Object> beforeData) { this.beforeData = beforeData; }
    
    public Map<String, Object> getAfterData() { return afterData; }
    public void setAfterData(Map<String, Object> afterData) { this.afterData = afterData; }
}