package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ReminderRequest;
import com.pos.inventsight.dto.ReminderResponse;
import com.pos.inventsight.model.sql.Reminder;
import com.pos.inventsight.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/calendar")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CalendarController {
    
    @Autowired
    private CalendarService calendarService;
    
    // GET /api/calendar/daily-activities - Get daily activities for date range
    @GetMapping("/daily-activities")
    public ResponseEntity<?> getDailyActivities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📅 InventSight - Fetching daily activities from " + startDate + " to " + endDate + " for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            Map<String, Object> dailyActivities = calendarService.getDailyActivities(startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("dailyActivities", dailyActivities);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Daily activities retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching daily activities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch daily activities: " + e.getMessage()));
        }
    }
    
    // GET /api/calendar/reminders - Get all reminders
    @GetMapping("/reminders")
    public ResponseEntity<?> getAllReminders(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false, defaultValue = "false") boolean includeCompleted,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📝 InventSight - Fetching reminders for user: " + username);
            
            List<Reminder> reminders;
            
            if (type != null && !type.isEmpty()) {
                reminders = calendarService.getRemindersByType(type);
            } else if (priority != null && !priority.isEmpty()) {
                reminders = calendarService.getRemindersByPriority(priority);
            } else if (includeCompleted) {
                reminders = calendarService.getAllReminders();
            } else {
                reminders = calendarService.getAllActiveReminders().stream()
                    .filter(r -> !r.getIsCompleted())
                    .collect(Collectors.toList());
            }
            
            List<ReminderResponse> reminderResponses = reminders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("reminders", reminderResponses);
            response.put("total", reminderResponses.size());
            response.put("filters", Map.of(
                "type", type != null ? type : "all",
                "priority", priority != null ? priority : "all",
                "includeCompleted", includeCompleted
            ));
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Reminders retrieved: " + reminderResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching reminders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch reminders: " + e.getMessage()));
        }
    }
    
    // POST /api/calendar/reminders - Create reminder
    @PostMapping("/reminders")
    public ResponseEntity<?> createReminder(@Valid @RequestBody ReminderRequest reminderRequest, 
                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("➕ InventSight - Creating reminder for user: " + username);
            System.out.println("📝 Reminder title: " + reminderRequest.getTitle());
            
            Reminder reminder = convertFromRequest(reminderRequest);
            Reminder createdReminder = calendarService.createReminder(reminder, username);
            ReminderResponse reminderResponse = convertToResponse(createdReminder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("reminder", reminderResponse);
            response.put("message", "Reminder created successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Reminder created successfully: " + createdReminder.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error creating reminder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to create reminder: " + e.getMessage()));
        }
    }
    
    // PUT /api/calendar/reminders/{id} - Update reminder
    @PutMapping("/reminders/{id}")
    public ResponseEntity<?> updateReminder(@PathVariable Long id, 
                                          @Valid @RequestBody ReminderRequest reminderRequest, 
                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 InventSight - Updating reminder ID: " + id + " for user: " + username);
            
            Reminder reminderUpdates = convertFromRequest(reminderRequest);
            Reminder updatedReminder = calendarService.updateReminder(id, reminderUpdates, username);
            ReminderResponse reminderResponse = convertToResponse(updatedReminder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("reminder", reminderResponse);
            response.put("message", "Reminder updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Reminder updated successfully: " + updatedReminder.getTitle());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error updating reminder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update reminder: " + e.getMessage()));
        }
    }
    
    // DELETE /api/calendar/reminders/{id} - Delete reminder
    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Deleting reminder ID: " + id + " for user: " + username);
            
            calendarService.deleteReminder(id, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reminder deleted successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Reminder deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error deleting reminder: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to delete reminder: " + e.getMessage()));
        }
    }
    
    // GET /api/calendar/reminders/today - Get today's reminders
    @GetMapping("/reminders/today")
    public ResponseEntity<?> getTodaysReminders(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📅 InventSight - Fetching today's reminders for user: " + username);
            
            List<Reminder> todaysReminders = calendarService.getTodaysReminders();
            List<ReminderResponse> reminderResponses = todaysReminders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("reminders", reminderResponses);
            response.put("count", reminderResponses.size());
            response.put("date", LocalDate.now());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Today's reminders retrieved: " + reminderResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching today's reminders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch today's reminders: " + e.getMessage()));
        }
    }
    
    // GET /api/calendar/reminders/overdue - Get past due reminders
    @GetMapping("/reminders/overdue")
    public ResponseEntity<?> getPastDueReminders(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("⚠️ InventSight - Fetching past due reminders for user: " + username);
            
            List<Reminder> pastDueReminders = calendarService.getPastDueReminders();
            List<ReminderResponse> reminderResponses = pastDueReminders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("reminders", reminderResponses);
            response.put("count", reminderResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Past due reminders retrieved: " + reminderResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching past due reminders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch past due reminders: " + e.getMessage()));
        }
    }
    
    // GET /api/calendar/activities-by-date - Get activities for specific date
    @GetMapping("/activities-by-date")
    public ResponseEntity<?> getActivitiesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📅 InventSight - Fetching activities for date: " + date + " for user: " + username);
            
            List<?> activities = calendarService.getActivitiesForDate(date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activities);
            response.put("date", date);
            response.put("count", activities.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Activities for date retrieved: " + activities.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching activities by date: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch activities by date: " + e.getMessage()));
        }
    }
    
    // GET /api/calendar/statistics - Get calendar statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getCalendarStatistics(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching calendar statistics for user: " + username);
            
            Map<String, Object> statistics = calendarService.getCalendarStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("statistics", statistics);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Calendar statistics retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching calendar statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch calendar statistics: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private ReminderResponse convertToResponse(Reminder reminder) {
        ReminderResponse response = new ReminderResponse(
            reminder.getId(),
            reminder.getTitle(),
            reminder.getDescription(),
            reminder.getReminderDateTime(),
            reminder.getReminderType(),
            reminder.getIsCompleted(),
            reminder.getIsActive(),
            reminder.getPriority(),
            reminder.getRelatedEntityType(),
            reminder.getRelatedEntityId(),
            reminder.getCreatedAt(),
            reminder.getUpdatedAt(),
            reminder.getCreatedBy(),
            reminder.getUpdatedBy()
        );
        
        // Set derived fields
        response.setIsPastDue(reminder.isPastDue());
        response.setIsDueToday(reminder.isDueToday());
        response.setIsDueSoon(reminder.isDueSoon());
        
        return response;
    }
    
    private Reminder convertFromRequest(ReminderRequest request) {
        Reminder reminder = new Reminder();
        reminder.setTitle(request.getTitle());
        reminder.setDescription(request.getDescription());
        reminder.setReminderDateTime(request.getReminderDateTime());
        reminder.setReminderType(request.getReminderType());
        reminder.setPriority(request.getPriority());
        reminder.setRelatedEntityType(request.getRelatedEntityType());
        reminder.setRelatedEntityId(request.getRelatedEntityId());
        return reminder;
    }
    
    // GET /api/calendar/events - Get all calendar events
    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📅 InventSight - Fetching calendar events for user: " + username);
            
            List<Map<String, Object>> events;
            
            if (startDate != null && endDate != null) {
                events = getEventsForDateRange(startDate, endDate);
                System.out.println("📅 Fetching events from " + startDate + " to " + endDate);
            } else if (startDate != null) {
                events = getEventsForDate(startDate);
                System.out.println("📅 Fetching events for date: " + startDate);
            } else {
                events = getAllEvents();
                System.out.println("📅 Fetching all events");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("events", events);
            response.put("count", events.size());
            response.put("dateRange", Map.of(
                "startDate", startDate != null ? startDate : "all",
                "endDate", endDate != null ? endDate : "all"
            ));
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Calendar events retrieved: " + events.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching calendar events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch calendar events: " + e.getMessage()));
        }
    }
    
    // POST /api/calendar/events - Create new calendar event
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@Valid @RequestBody Map<String, Object> eventRequest,
                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("➕ InventSight - Creating calendar event for user: " + username);
            
            Map<String, Object> newEvent = createNewEvent(eventRequest, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("event", newEvent);
            response.put("message", "Event created successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Calendar event created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error creating calendar event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to create calendar event: " + e.getMessage()));
        }
    }
    
    // PUT /api/calendar/events/{id} - Update calendar event
    @PutMapping("/events/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                       @Valid @RequestBody Map<String, Object> eventRequest,
                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 InventSight - Updating calendar event ID: " + id + " for user: " + username);
            
            Map<String, Object> updatedEvent = updateExistingEvent(id, eventRequest, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("event", updatedEvent);
            response.put("message", "Event updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Calendar event updated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error updating calendar event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update calendar event: " + e.getMessage()));
        }
    }
    
    // DELETE /api/calendar/events/{id} - Delete calendar event
    @DeleteMapping("/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ InventSight - Deleting calendar event ID: " + id + " for user: " + username);
            
            deleteExistingEvent(id, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Event deleted successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Calendar event deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error deleting calendar event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to delete calendar event: " + e.getMessage()));
        }
    }
    
    // Helper methods for events
    private List<Map<String, Object>> getAllEvents() {
        // Mock implementation - in real scenario would fetch from database
        return List.of();
    }
    
    private List<Map<String, Object>> getEventsForDate(LocalDate date) {
        // Mock implementation - in real scenario would fetch from database
        return List.of();
    }
    
    private List<Map<String, Object>> getEventsForDateRange(LocalDate startDate, LocalDate endDate) {
        // Mock implementation - in real scenario would fetch from database  
        return List.of();
    }
    
    private Map<String, Object> createNewEvent(Map<String, Object> eventRequest, String username) {
        // Mock implementation - in real scenario would save to database
        Map<String, Object> newEvent = new HashMap<>(eventRequest);
        newEvent.put("id", System.currentTimeMillis());
        newEvent.put("createdBy", username);
        newEvent.put("createdAt", LocalDateTime.now());
        return newEvent;
    }
    
    private Map<String, Object> updateExistingEvent(Long id, Map<String, Object> eventRequest, String username) {
        // Mock implementation - in real scenario would update in database
        Map<String, Object> updatedEvent = new HashMap<>(eventRequest);
        updatedEvent.put("id", id);
        updatedEvent.put("updatedBy", username);
        updatedEvent.put("updatedAt", LocalDateTime.now());
        return updatedEvent;
    }
    
    private void deleteExistingEvent(Long id, String username) {
        // Mock implementation - in real scenario would delete from database
        System.out.println("Event ID " + id + " marked for deletion by " + username);
    }
}