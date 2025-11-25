package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.EventType;
import com.pos.inventsight.model.sql.EventPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EventRequest {
    
    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Event title cannot exceed 200 characters")
    private String title;
    
    private String description;
    
    @NotNull(message = "Start date and time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateTime;
    
    @NotNull(message = "End date and time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDateTime;
    
    private String location;
    
    private EventType type = EventType.MEETING;
    
    private EventPriority priority = EventPriority.MEDIUM;
    
    private Boolean isAllDay = false;
    
    private Boolean isRecurring = false;
    
    private String recurrencePattern;
    
    private Integer reminderMinutes = 15;
    
    private List<UUID> attendeeIds;
    
    // Constructors
    public EventRequest() {}
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    
    public EventPriority getPriority() { return priority; }
    public void setPriority(EventPriority priority) { this.priority = priority; }
    
    public Boolean getIsAllDay() { return isAllDay; }
    public void setIsAllDay(Boolean isAllDay) { this.isAllDay = isAllDay; }
    
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }
    
    public String getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }
    
    public Integer getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(Integer reminderMinutes) { this.reminderMinutes = reminderMinutes; }
    
    public List<UUID> getAttendeeIds() { return attendeeIds; }
    public void setAttendeeIds(List<UUID> attendeeIds) { this.attendeeIds = attendeeIds; }
}