package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.EventRequest;
import com.pos.inventsight.dto.EventResponse;
import com.pos.inventsight.model.sql.Event;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.EventService;
import com.pos.inventsight.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private UserService userService;
    
    // GET /api/events - Get all events for authenticated user
    @GetMapping
    public ResponseEntity<?> getAllEvents(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Fetching events for user: " + username);
            
            List<Event> events = eventService.getEventsByUser(user.getId());
            List<EventResponse> eventResponses = events.stream()
                    .map(EventResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", eventResponses);
            response.put("totalCount", eventResponses.size());
            response.put("upcomingCount", eventResponses.stream()
                    .mapToInt(e -> e.isUpcoming() ? 1 : 0).sum());
            response.put("activeCount", eventResponses.stream()
                    .mapToInt(e -> e.isCurrentlyActive() ? 1 : 0).sum());
            
            System.out.println("✅ Retrieved " + events.size() + " events for user: " + username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching events: " + e.getMessage()));
        }
    }
    
    // GET /api/events/{id} - Get specific event
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Fetching event ID: " + id + " for user: " + username);
            
            Event event = eventService.getEventById(id);
            
            // Check if user has access to this event (creator, attendee, or admin)
            boolean hasAccess = event.getCreatedBy().getId().equals(user.getId()) ||
                               event.getAttendees().stream().anyMatch(attendee -> attendee.getId().equals(user.getId())) ||
                               user.getRole().name().equals("ADMIN");
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: You don't have permission to view this event"));
            }
            
            EventResponse eventResponse = new EventResponse(event);
            
            System.out.println("✅ Retrieved event: " + event.getTitle());
            return ResponseEntity.ok(eventResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Event not found with ID: " + id));
        }
    }
    
    // POST /api/events - Create new event
    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest eventRequest, 
                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Creating new event: " + eventRequest.getTitle() + " for user: " + username);
            System.out.println("🗓️ Event duration: " + eventRequest.getStartDateTime() + " to " + eventRequest.getEndDateTime());
            
            Event event = eventService.createEvent(eventRequest, user.getId());
            EventResponse eventResponse = new EventResponse(event);
            
            System.out.println("✅ Created event: " + event.getTitle() + " with ID: " + event.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error creating event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error creating event: " + e.getMessage()));
        }
    }
    
    // PUT /api/events/{id} - Update existing event
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, 
                                       @Valid @RequestBody EventRequest eventRequest,
                                       Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Updating event ID: " + id + " by user: " + username);
            
            Event updatedEvent = eventService.updateEvent(id, eventRequest, user.getId());
            EventResponse eventResponse = new EventResponse(updatedEvent);
            
            System.out.println("✅ Updated event: " + updatedEvent.getTitle());
            return ResponseEntity.ok(eventResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating event: " + e.getMessage()));
        }
    }
    
    // DELETE /api/events/{id} - Delete event
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Deleting event ID: " + id + " by user: " + username);
            
            Event event = eventService.getEventById(id);
            eventService.deleteEvent(id, user.getId());
            
            System.out.println("✅ Deleted event: " + event.getTitle());
            return ResponseEntity.ok(new ApiResponse(true, "Event deleted successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Error deleting event: " + e.getMessage()));
        }
    }
    
    // GET /api/events/calendar/{year}/{month} - Get events for specific month
    @GetMapping("/calendar/{year}/{month}")
    public ResponseEntity<?> getEventsForMonth(@PathVariable int year, 
                                             @PathVariable int month,
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Fetching calendar events for " + year + "-" + month + " for user: " + username);
            
            if (month < 1 || month > 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Invalid month. Must be between 1 and 12"));
            }
            
            List<Event> events = eventService.getEventsForMonth(year, month, user.getId());
            List<EventResponse> eventResponses = events.stream()
                    .map(EventResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("events", eventResponses);
            response.put("totalCount", eventResponses.size());
            
            System.out.println("✅ Retrieved " + events.size() + " events for " + year + "-" + month);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching calendar events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching calendar events: " + e.getMessage()));
        }
    }
    
    // POST /api/events/{id}/attendees - Add attendees to event
    @PostMapping("/{id}/attendees")
    public ResponseEntity<?> addAttendeesToEvent(@PathVariable Long id,
                                               @RequestBody Map<String, List<Long>> attendeeRequest,
                                               Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            List<Long> attendeeIds = attendeeRequest.get("attendeeIds");
            if (attendeeIds == null || attendeeIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "attendeeIds is required and cannot be empty"));
            }
            
            System.out.println("📅 InventSight - Adding " + attendeeIds.size() + " attendees to event ID: " + id);
            
            Event updatedEvent = eventService.addAttendeesToEvent(id, attendeeIds, user.getId());
            EventResponse eventResponse = new EventResponse(updatedEvent);
            
            System.out.println("✅ Added attendees to event: " + updatedEvent.getTitle());
            return ResponseEntity.ok(eventResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding attendees to event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error adding attendees to event: " + e.getMessage()));
        }
    }
    
    // Additional utility endpoints
    
    // GET /api/events/upcoming - Get upcoming events for user
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvents(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Fetching upcoming events for user: " + username);
            
            List<Event> events = eventService.getUpcomingEvents(user.getId());
            List<EventResponse> eventResponses = events.stream()
                    .map(EventResponse::new)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Retrieved " + events.size() + " upcoming events");
            return ResponseEntity.ok(eventResponses);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching upcoming events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching upcoming events: " + e.getMessage()));
        }
    }
    
    // GET /api/events/today - Get today's events for user
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysEvents(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Fetching today's events for user: " + username);
            
            List<Event> events = eventService.getTodaysEvents(user.getId());
            List<EventResponse> eventResponses = events.stream()
                    .map(EventResponse::new)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Retrieved " + events.size() + " today's events");
            return ResponseEntity.ok(eventResponses);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching today's events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching today's events: " + e.getMessage()));
        }
    }
    
    // GET /api/events/search - Search events
    @GetMapping("/search")
    public ResponseEntity<?> searchEvents(@RequestParam String query, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📅 InventSight - Searching events with query: " + query + " for user: " + username);
            
            List<Event> events = eventService.searchEvents(query, user.getId());
            List<EventResponse> eventResponses = events.stream()
                    .map(EventResponse::new)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Found " + events.size() + " events matching query: " + query);
            return ResponseEntity.ok(eventResponses);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error searching events: " + e.getMessage()));
        }
    }
}