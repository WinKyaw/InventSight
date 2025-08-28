package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
public class Reminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 200)
    private String title;
    
    @Size(max = 1000)
    private String description;
    
    @NotNull
    @Column(name = "reminder_date_time")
    private LocalDateTime reminderDateTime;
    
    @NotBlank
    @Column(name = "reminder_type")
    private String reminderType; // ORDER, MEETING, MAINTENANCE, RESTOCK, OTHER
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Size(max = 50)
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    @Size(max = 100)
    @Column(name = "related_entity_type")
    private String relatedEntityType; // PRODUCT, ORDER, EMPLOYEE, etc.
    
    @Column(name = "related_entity_id")
    private String relatedEntityId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Constructors
    public Reminder() {}
    
    public Reminder(String title, String description, LocalDateTime reminderDateTime, 
                   String reminderType, String createdBy) {
        this.title = title;
        this.description = description;
        this.reminderDateTime = reminderDateTime;
        this.reminderType = reminderType;
        this.createdBy = createdBy;
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
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business Logic Methods
    public boolean isPastDue() {
        return reminderDateTime.isBefore(LocalDateTime.now()) && !isCompleted;
    }
    
    public boolean isDueToday() {
        LocalDateTime now = LocalDateTime.now();
        return reminderDateTime.toLocalDate().equals(now.toLocalDate());
    }
    
    public boolean isDueSoon() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        return reminderDateTime.isBefore(tomorrow) && reminderDateTime.isAfter(LocalDateTime.now());
    }
}