package com.pos.inventsight.dto;

import java.time.LocalDateTime;

public class ReminderResponse {
    
    private Long id;
    private String title;
    private String description;
    private LocalDateTime reminderDateTime;
    private String reminderType;
    private Boolean isCompleted;
    private Boolean isActive;
    private String priority;
    private String relatedEntityType;
    private String relatedEntityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Derived fields
    private Boolean isPastDue;
    private Boolean isDueToday;
    private Boolean isDueSoon;
    
    // Constructors
    public ReminderResponse() {}
    
    public ReminderResponse(Long id, String title, String description, LocalDateTime reminderDateTime,
                           String reminderType, Boolean isCompleted, Boolean isActive, String priority,
                           String relatedEntityType, String relatedEntityId, LocalDateTime createdAt,
                           LocalDateTime updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reminderDateTime = reminderDateTime;
        this.reminderType = reminderType;
        this.isCompleted = isCompleted;
        this.isActive = isActive;
        this.priority = priority;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) { this.reminderDateTime = reminderDateTime; }
    
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String relatedEntityType) { this.relatedEntityType = relatedEntityType; }
    
    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Boolean getIsPastDue() { return isPastDue; }
    public void setIsPastDue(Boolean isPastDue) { this.isPastDue = isPastDue; }
    
    public Boolean getIsDueToday() { return isDueToday; }
    public void setIsDueToday(Boolean isDueToday) { this.isDueToday = isDueToday; }
    
    public Boolean getIsDueSoon() { return isDueSoon; }
    public void setIsDueSoon(Boolean isDueSoon) { this.isDueSoon = isDueSoon; }
}