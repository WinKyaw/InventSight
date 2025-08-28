package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Event;
import com.pos.inventsight.model.sql.EventStatus;
import com.pos.inventsight.model.sql.EventType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.EventRepository;
import com.pos.inventsight.dto.EventRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // CRUD Operations
    public Event createEvent(EventRequest eventRequest, Long createdByUserId) {
        User createdBy = userService.getUserById(createdByUserId);
        
        // Validate event dates
        if (eventRequest.getEndDateTime().isBefore(eventRequest.getStartDateTime())) {
            throw new ValidationException("End date/time cannot be before start date/time");
        }
        
        Event event = new Event(
            eventRequest.getTitle(),
            eventRequest.getDescription(),
            eventRequest.getStartDateTime(),
            eventRequest.getEndDateTime(),
            createdBy
        );
        
        // Set optional fields
        if (eventRequest.getLocation() != null) {
            event.setLocation(eventRequest.getLocation());
        }
        if (eventRequest.getType() != null) {
            event.setType(eventRequest.getType());
        }
        if (eventRequest.getPriority() != null) {
            event.setPriority(eventRequest.getPriority());
        }
        if (eventRequest.getIsAllDay() != null) {
            event.setIsAllDay(eventRequest.getIsAllDay());
        }
        if (eventRequest.getIsRecurring() != null) {
            event.setIsRecurring(eventRequest.getIsRecurring());
        }
        if (eventRequest.getRecurrencePattern() != null) {
            event.setRecurrencePattern(eventRequest.getRecurrencePattern());
        }
        if (eventRequest.getReminderMinutes() != null) {
            event.setReminderMinutes(eventRequest.getReminderMinutes());
        }
        
        // Add attendees if provided
        if (eventRequest.getAttendeeIds() != null && !eventRequest.getAttendeeIds().isEmpty()) {
            List<User> attendees = new ArrayList<>();
            for (Long attendeeId : eventRequest.getAttendeeIds()) {
                try {
                    User attendee = userService.getUserById(attendeeId);
                    attendees.add(attendee);
                } catch (ResourceNotFoundException e) {
                    // Skip invalid attendee IDs
                    System.out.println("⚠️ Skipping invalid attendee ID: " + attendeeId);
                }
            }
            event.setAttendees(attendees);
        }
        
        Event savedEvent = eventRepository.save(event);
        
        // Log activity
        activityLogService.logActivity(
            createdByUserId.toString(),
            createdBy.getUsername(),
            "EVENT_CREATED",
            "Event",
            "Created event: " + event.getTitle()
        );
        
        return savedEvent;
    }
    
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
    }
    
    public List<Event> getEventsByUser(Long userId) {
        return eventRepository.findEventsByUserIdAndStatus(userId, EventStatus.ACTIVE);
    }
    
    public Event updateEvent(Long eventId, EventRequest eventRequest, Long userId) {
        Event existingEvent = getEventById(eventId);
        User currentUser = userService.getUserById(userId);
        
        // Check if user can update this event (creator or admin)
        if (!existingEvent.getCreatedBy().getId().equals(userId) && 
            !currentUser.getRole().name().equals("ADMIN")) {
            throw new ValidationException("You can only update events you created");
        }
        
        // Validate event dates
        if (eventRequest.getEndDateTime().isBefore(eventRequest.getStartDateTime())) {
            throw new ValidationException("End date/time cannot be before start date/time");
        }
        
        // Update fields
        existingEvent.setTitle(eventRequest.getTitle());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStartDateTime(eventRequest.getStartDateTime());
        existingEvent.setEndDateTime(eventRequest.getEndDateTime());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setType(eventRequest.getType());
        existingEvent.setPriority(eventRequest.getPriority());
        existingEvent.setIsAllDay(eventRequest.getIsAllDay());
        existingEvent.setIsRecurring(eventRequest.getIsRecurring());
        existingEvent.setRecurrencePattern(eventRequest.getRecurrencePattern());
        existingEvent.setReminderMinutes(eventRequest.getReminderMinutes());
        existingEvent.setUpdatedAt(LocalDateTime.now());
        
        // Update attendees if provided
        if (eventRequest.getAttendeeIds() != null) {
            List<User> attendees = new ArrayList<>();
            for (Long attendeeId : eventRequest.getAttendeeIds()) {
                try {
                    User attendee = userService.getUserById(attendeeId);
                    attendees.add(attendee);
                } catch (ResourceNotFoundException e) {
                    // Skip invalid attendee IDs
                    System.out.println("⚠️ Skipping invalid attendee ID: " + attendeeId);
                }
            }
            existingEvent.setAttendees(attendees);
        }
        
        Event updatedEvent = eventRepository.save(existingEvent);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(),
            currentUser.getUsername(),
            "EVENT_UPDATED",
            "Event",
            "Updated event: " + existingEvent.getTitle()
        );
        
        return updatedEvent;
    }
    
    public void deleteEvent(Long eventId, Long userId) {
        Event event = getEventById(eventId);
        User currentUser = userService.getUserById(userId);
        
        // Check if user can delete this event (creator or admin)
        if (!event.getCreatedBy().getId().equals(userId) && 
            !currentUser.getRole().name().equals("ADMIN")) {
            throw new ValidationException("You can only delete events you created");
        }
        
        // Soft delete by setting status to CANCELLED
        event.setStatus(EventStatus.CANCELLED);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(),
            currentUser.getUsername(),
            "EVENT_DELETED",
            "Event",
            "Deleted event: " + event.getTitle()
        );
    }
    
    // Calendar specific methods
    public List<Event> getEventsForMonth(int year, int month, Long userId) {
        return eventRepository.findEventsByYearMonthAndUser(year, month, userId);
    }
    
    public List<Event> getEventsInDateRange(LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        List<Event> userEvents = getEventsByUser(userId);
        return userEvents.stream()
                .filter(event -> !event.getStartDateTime().isAfter(endDate) && 
                               !event.getEndDateTime().isBefore(startDate))
                .toList();
    }
    
    // Attendee management
    public Event addAttendeesToEvent(Long eventId, List<Long> attendeeIds, Long userId) {
        Event event = getEventById(eventId);
        User currentUser = userService.getUserById(userId);
        
        // Check if user can modify this event (creator or admin)
        if (!event.getCreatedBy().getId().equals(userId) && 
            !currentUser.getRole().name().equals("ADMIN")) {
            throw new ValidationException("You can only modify events you created");
        }
        
        // Add new attendees
        for (Long attendeeId : attendeeIds) {
            try {
                User attendee = userService.getUserById(attendeeId);
                event.addAttendee(attendee);
            } catch (ResourceNotFoundException e) {
                System.out.println("⚠️ Skipping invalid attendee ID: " + attendeeId);
            }
        }
        
        event.setUpdatedAt(LocalDateTime.now());
        Event updatedEvent = eventRepository.save(event);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(),
            currentUser.getUsername(),
            "EVENT_ATTENDEES_ADDED",
            "Event",
            "Added " + attendeeIds.size() + " attendees to event: " + event.getTitle()
        );
        
        return updatedEvent;
    }
    
    // Utility methods
    public List<Event> getUpcomingEvents(Long userId) {
        return eventRepository.findUpcomingEventsByUser(userId, LocalDateTime.now());
    }
    
    public List<Event> getTodaysEvents(Long userId) {
        return eventRepository.findTodaysEventsByUser(userId);
    }
    
    public List<Event> searchEvents(String searchTerm, Long userId) {
        return eventRepository.searchEventsByUser(searchTerm, userId);
    }
    
    public long getActiveEventCount(Long userId) {
        return eventRepository.countActiveEventsByUser(userId);
    }
    
    public List<Event> getOverlappingEvents(LocalDateTime startDateTime, LocalDateTime endDateTime, Long excludeEventId) {
        return eventRepository.findOverlappingEvents(startDateTime, endDateTime, excludeEventId);
    }
}