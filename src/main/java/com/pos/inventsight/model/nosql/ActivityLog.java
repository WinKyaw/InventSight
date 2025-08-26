package com.pos.inventsight.model.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "activity_logs")
public class ActivityLog {
    
    @Id
    private String id;
    
    private String userId;
    private String username = "WinKyaw";
    private String action;
    private String entityType; // PRODUCT, SALE, EMPLOYEE, USER, etc.
    private String entityId;
    private String description;
    
    private Map<String, Object> metadata;
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    
    private LocalDateTime timestamp;
    private String severity = "INFO"; // INFO, WARN, ERROR
    private String module; // INVENTORY, SALES, EMPLOYEE, AUTH
    
    // Constructors
    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
        this.severity = "INFO";
    }
    
    public ActivityLog(String userId, String username, String action, String entityType, String description) {
        this();
        this.userId = userId;
        this.username = username != null ? username : "WinKyaw";
        this.action = action;
        this.entityType = entityType;
        this.description = description;
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
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, Object> getBeforeData() { return beforeData; }
    public void setBeforeData(Map<String, Object> beforeData) { this.beforeData = beforeData; }
    
    public Map<String, Object> getAfterData() { return afterData; }
    public void setAfterData(Map<String, Object> afterData) { this.afterData = afterData; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
}