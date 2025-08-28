package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ReminderRequest {
    
    @NotBlank(message = "Reminder title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Reminder date and time is required")
    private LocalDateTime reminderDateTime;
    
    @NotBlank(message = "Reminder type is required")
    private String reminderType; // ORDER, MEETING, MAINTENANCE, RESTOCK, OTHER
    
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    private String relatedEntityType;
    
    private String relatedEntityId;
    
    // Constructors
    public ReminderRequest() {}
    
    public ReminderRequest(String title, String description, LocalDateTime reminderDateTime, 
                          String reminderType, String priority) {
        this.title = title;
        this.description = description;
        this.reminderDateTime = reminderDateTime;
        this.reminderType = reminderType;
        this.priority = priority;
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) { this.reminderDateTime = reminderDateTime; }
    
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String relatedEntityType) { this.relatedEntityType = relatedEntityType; }
    
    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }
}