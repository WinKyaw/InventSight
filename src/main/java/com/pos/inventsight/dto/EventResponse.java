package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Event;
import com.pos.inventsight.model.sql.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventResponse {
    
    private Long id;
    private String title;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDateTime;
    
    private String location;
    private String type;
    private String priority;
    private Boolean isAllDay;
    private Boolean isRecurring;
    private String recurrencePattern;
    private Integer reminderMinutes;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private UserSummary createdBy;
    private List<UserSummary> attendees;
    
    private long durationInMinutes;
    private boolean isUpcoming;
    private boolean isCurrentlyActive;
    private int attendeeCount;
    
    // Constructor
    public EventResponse(Event event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.startDateTime = event.getStartDateTime();
        this.endDateTime = event.getEndDateTime();
        this.location = event.getLocation();
        this.type = event.getType().name();
        this.priority = event.getPriority().name();
        this.isAllDay = event.getIsAllDay();
        this.isRecurring = event.getIsRecurring();
        this.recurrencePattern = event.getRecurrencePattern();
        this.reminderMinutes = event.getReminderMinutes();
        this.status = event.getStatus().name();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getUpdatedAt();
        this.createdBy = new UserSummary(event.getCreatedBy());
        this.attendees = event.getAttendees().stream()
                .map(UserSummary::new)
                .collect(Collectors.toList());
        this.durationInMinutes = event.getDurationInMinutes();
        this.isUpcoming = event.isUpcoming();
        this.isCurrentlyActive = event.isCurrentlyActive();
        this.attendeeCount = event.getAttendeeCount();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public String getPriority() { return priority; }
    public Boolean getIsAllDay() { return isAllDay; }
    public Boolean getIsRecurring() { return isRecurring; }
    public String getRecurrencePattern() { return recurrencePattern; }
    public Integer getReminderMinutes() { return reminderMinutes; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UserSummary getCreatedBy() { return createdBy; }
    public List<UserSummary> getAttendees() { return attendees; }
    public long getDurationInMinutes() { return durationInMinutes; }
    public boolean isUpcoming() { return isUpcoming; }
    public boolean isCurrentlyActive() { return isCurrentlyActive; }
    public int getAttendeeCount() { return attendeeCount; }
    
    // Inner class for user summary
    public static class UserSummary {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        
        public UserSummary(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.role = user.getRole().name();
        }
        
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getFullName() { return firstName + " " + lastName; }
    }
}