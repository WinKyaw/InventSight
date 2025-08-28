package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Reminder;
import com.pos.inventsight.model.sql.Event;
import com.pos.inventsight.repository.sql.ReminderRepository;
import com.pos.inventsight.repository.sql.EventRepository;
import com.pos.inventsight.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class CalendarService {
    
    @Autowired
    private ReminderRepository reminderRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // Reminder CRUD Operations
    public Reminder createReminder(Reminder reminder, String createdBy) {
        System.out.println("📝 InventSight - Creating new reminder: " + reminder.getTitle());
        System.out.println("👤 Created by: " + createdBy);
        
        reminder.setCreatedBy(createdBy);
        reminder.setUpdatedBy(createdBy);
        reminder.setCreatedAt(LocalDateTime.now());
        reminder.setUpdatedAt(LocalDateTime.now());
        
        if (reminder.getPriority() == null) {
            reminder.setPriority("MEDIUM");
        }
        
        Reminder savedReminder = reminderRepository.save(reminder);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            createdBy != null ? createdBy : "WinKyaw",
            "REMINDER_CREATED", 
            "REMINDER", 
            "Created new reminder: " + savedReminder.getTitle()
        );
        
        System.out.println("✅ InventSight - Reminder created successfully with ID: " + savedReminder.getId());
        return savedReminder;
    }
    
    public Reminder updateReminder(Long reminderId, Reminder reminderUpdates, String updatedBy) {
        System.out.println("🔄 InventSight - Updating reminder ID: " + reminderId);
        System.out.println("👤 Updated by: " + updatedBy);
        
        Reminder existingReminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with ID: " + reminderId));
        
        // Update fields
        if (reminderUpdates.getTitle() != null) {
            existingReminder.setTitle(reminderUpdates.getTitle());
        }
        if (reminderUpdates.getDescription() != null) {
            existingReminder.setDescription(reminderUpdates.getDescription());
        }
        if (reminderUpdates.getReminderDateTime() != null) {
            existingReminder.setReminderDateTime(reminderUpdates.getReminderDateTime());
        }
        if (reminderUpdates.getReminderType() != null) {
            existingReminder.setReminderType(reminderUpdates.getReminderType());
        }
        if (reminderUpdates.getPriority() != null) {
            existingReminder.setPriority(reminderUpdates.getPriority());
        }
        if (reminderUpdates.getIsCompleted() != null) {
            existingReminder.setIsCompleted(reminderUpdates.getIsCompleted());
        }
        if (reminderUpdates.getRelatedEntityType() != null) {
            existingReminder.setRelatedEntityType(reminderUpdates.getRelatedEntityType());
        }
        if (reminderUpdates.getRelatedEntityId() != null) {
            existingReminder.setRelatedEntityId(reminderUpdates.getRelatedEntityId());
        }
        
        existingReminder.setUpdatedBy(updatedBy);
        existingReminder.setUpdatedAt(LocalDateTime.now());
        
        Reminder savedReminder = reminderRepository.save(existingReminder);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            updatedBy != null ? updatedBy : "WinKyaw",
            "REMINDER_UPDATED", 
            "REMINDER", 
            "Updated reminder: " + savedReminder.getTitle()
        );
        
        System.out.println("✅ InventSight - Reminder updated successfully");
        return savedReminder;
    }
    
    public Reminder getReminderById(Long reminderId) {
        return reminderRepository.findById(reminderId)
            .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with ID: " + reminderId));
    }
    
    public List<Reminder> getAllActiveReminders() {
        return reminderRepository.findByIsActiveTrue();
    }
    
    public List<Reminder> getAllReminders() {
        return reminderRepository.findAll();
    }
    
    public void deleteReminder(Long reminderId, String deletedBy) {
        System.out.println("🗑️ InventSight - Deleting reminder ID: " + reminderId);
        System.out.println("👤 Deleted by: " + deletedBy);
        
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with ID: " + reminderId));
        
        // Soft delete - mark as inactive
        reminder.setIsActive(false);
        reminder.setUpdatedBy(deletedBy);
        reminder.setUpdatedAt(LocalDateTime.now());
        
        reminderRepository.save(reminder);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            deletedBy != null ? deletedBy : "WinKyaw",
            "REMINDER_DELETED", 
            "REMINDER", 
            "Deleted reminder: " + reminder.getTitle()
        );
        
        System.out.println("✅ InventSight - Reminder deleted successfully (soft delete)");
    }
    
    // Calendar-specific operations
    public Map<String, Object> getDailyActivities(LocalDate startDate, LocalDate endDate) {
        System.out.println("📅 InventSight - Getting daily activities from " + startDate + " to " + endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Reminder> reminders = reminderRepository.findByReminderDateTimeBetween(startDateTime, endDateTime);
        List<Event> events = eventRepository.findEventsInDateRange(startDateTime, endDateTime);
        
        Map<String, Object> dailyActivities = new HashMap<>();
        dailyActivities.put("reminders", reminders);
        dailyActivities.put("events", events);
        dailyActivities.put("startDate", startDate);
        dailyActivities.put("endDate", endDate);
        dailyActivities.put("totalReminders", reminders.size());
        dailyActivities.put("totalEvents", events.size());
        
        System.out.println("✅ InventSight - Daily activities retrieved: " + reminders.size() + " reminders, " + events.size() + " events");
        return dailyActivities;
    }
    
    public List<Reminder> getTodaysReminders() {
        return reminderRepository.findTodaysReminders();
    }
    
    public List<Reminder> getPastDueReminders() {
        return reminderRepository.findPastDueReminders(LocalDateTime.now());
    }
    
    public List<Reminder> getDueSoonReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        return reminderRepository.findDueSoonReminders(now, tomorrow);
    }
    
    public List<Reminder> getRemindersByType(String reminderType) {
        return reminderRepository.findByReminderType(reminderType);
    }
    
    public List<Reminder> getRemindersByPriority(String priority) {
        return reminderRepository.findByPriority(priority);
    }
    
    public List<Reminder> getRemindersByUser(String username) {
        return reminderRepository.findByCreatedBy(username);
    }
    
    // Analytics
    public Map<String, Object> getCalendarStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActiveReminders", reminderRepository.countActiveReminders());
        stats.put("pendingReminders", reminderRepository.countPendingReminders());
        stats.put("todaysReminders", getTodaysReminders().size());
        stats.put("pastDueReminders", getPastDueReminders().size());
        stats.put("dueSoonReminders", getDueSoonReminders().size());
        
        return stats;
    }
    
    public List<Event> getActivitiesForDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return eventRepository.findEventsInDateRange(startOfDay, endOfDay);
    }
}