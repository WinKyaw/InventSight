package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "events")
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull
    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;
    
    @NotNull
    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;
    
    @Column(name = "location")
    private String location;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType type = EventType.MEETING;
    
    @Enumerated(EnumType.STRING)
    private EventPriority priority = EventPriority.MEDIUM;
    
    @Column(name = "is_all_day")
    private Boolean isAllDay = false;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;
    
    @Column(name = "recurrence_pattern")
    private String recurrencePattern;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_attendees",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> attendees = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes = 15;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.ACTIVE;
    
    // Constructors
    public Event() {}
    
    public Event(String title, String description, LocalDateTime startDateTime, 
                LocalDateTime endDateTime, User createdBy) {
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.createdBy = createdBy;
    }
    
    // Business Logic Methods
    public boolean isOverlapping(Event otherEvent) {
        return this.startDateTime.isBefore(otherEvent.endDateTime) && 
               this.endDateTime.isAfter(otherEvent.startDateTime);
    }
    
    public long getDurationInMinutes() {
        return java.time.Duration.between(startDateTime, endDateTime).toMinutes();
    }
    
    public boolean isUpcoming() {
        return this.startDateTime.isAfter(LocalDateTime.now());
    }
    
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return this.startDateTime.isBefore(now) && this.endDateTime.isAfter(now);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public List<User> getAttendees() { return attendees; }
    public void setAttendees(List<User> attendees) { this.attendees = attendees; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(Integer reminderMinutes) { this.reminderMinutes = reminderMinutes; }
    
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    
    // Helper methods for attendees
    public void addAttendee(User user) {
        if (!this.attendees.contains(user)) {
            this.attendees.add(user);
        }
    }
    
    public void removeAttendee(User user) {
        this.attendees.remove(user);
    }
    
    public boolean hasAttendee(User user) {
        return this.attendees.contains(user);
    }
    
    public int getAttendeeCount() {
        return this.attendees.size();
    }
}