package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ActivityResponse;
import com.pos.inventsight.model.nosql.ActivityLog;
import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ActivityController {
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    // GET /api/activities - Get all activities with pagination
    @GetMapping
    public ResponseEntity<?> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String username,
            Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            System.out.println("📋 InventSight - Fetching activities for user: " + currentUsername);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ActivityLog> activitiesPage = activityLogRepository.findAllByOrderByTimestampDesc(pageable);
            
            List<ActivityResponse> activityResponses = activitiesPage.getContent().stream()
                .filter(activity -> {
                    // Apply filters
                    if (module != null && !module.isEmpty() && !module.equals(activity.getModule())) {
                        return false;
                    }
                    if (action != null && !action.isEmpty() && !action.equals(activity.getAction())) {
                        return false;
                    }
                    if (entityType != null && !entityType.isEmpty() && !entityType.equals(activity.getEntityType())) {
                        return false;
                    }
                    if (username != null && !username.isEmpty() && !username.equals(activity.getUsername())) {
                        return false;
                    }
                    return true;
                })
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityResponses);
            response.put("currentPage", activitiesPage.getNumber());
            response.put("totalItems", activitiesPage.getTotalElements());
            response.put("totalPages", activitiesPage.getTotalPages());
            response.put("pageSize", activitiesPage.getSize());
            response.put("filters", Map.of(
                "module", module != null ? module : "all",
                "action", action != null ? action : "all",
                "entityType", entityType != null ? entityType : "all",
                "username", username != null ? username : "all"
            ));
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Activities retrieved: " + activityResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching activities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch activities: " + e.getMessage()));
        }
    }
    
    // GET /api/activities/recent - Get recent activities (last 50)
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentActivities(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📋 InventSight - Fetching recent activities for user: " + username);
            
            List<ActivityLog> recentActivities = activityLogRepository.findTop10ByOrderByTimestampDesc();
            List<ActivityResponse> activityResponses = recentActivities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityResponses);
            response.put("count", activityResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Recent activities retrieved: " + activityResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching recent activities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch recent activities: " + e.getMessage()));
        }
    }
    
    // GET /api/activities/{id} - Get activity by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getActivityById(@PathVariable String id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 InventSight - Fetching activity ID: " + id + " for user: " + username);
            
            Optional<ActivityLog> activityOpt = activityLogRepository.findById(id);
            if (activityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Activity not found with ID: " + id));
            }
            
            ActivityLog activity = activityOpt.get();
            ActivityResponse activityResponse = convertToResponse(activity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("activity", activityResponse);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Activity retrieved: " + activity.getDescription());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Activity not found: " + e.getMessage()));
        }
    }
    
    // GET /api/activities/by-user/{username} - Get activities by username
    @GetMapping("/by-user/{username}")
    public ResponseEntity<?> getActivitiesByUser(@PathVariable String username,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            System.out.println("👤 InventSight - Fetching activities for user: " + username + " requested by: " + currentUsername);
            
            List<ActivityLog> userActivities = activityLogRepository.findByUsernameOrderByTimestampDesc(username);
            
            // Apply pagination manually
            int start = page * size;
            int end = Math.min(start + size, userActivities.size());
            List<ActivityLog> paginatedActivities = userActivities.subList(start, end);
            
            List<ActivityResponse> activityResponses = paginatedActivities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityResponses);
            response.put("username", username);
            response.put("currentPage", page);
            response.put("totalItems", userActivities.size());
            response.put("totalPages", (int) Math.ceil((double) userActivities.size() / size));
            response.put("pageSize", size);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - User activities retrieved: " + activityResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching user activities: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch user activities: " + e.getMessage()));
        }
    }
    
    // GET /api/activities/by-date-range - Get activities by date range
    @GetMapping("/by-date-range")
    public ResponseEntity<?> getActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📅 InventSight - Fetching activities from " + startDate + " to " + endDate + " for user: " + username);
            
            List<ActivityLog> dateRangeActivities = activityLogRepository.findByTimestampBetween(startDate, endDate);
            List<ActivityResponse> activityResponses = dateRangeActivities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityResponses);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("count", activityResponses.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Date range activities retrieved: " + activityResponses.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching activities by date range: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch activities by date range: " + e.getMessage()));
        }
    }
    
    // Helper method to convert ActivityLog to ActivityResponse
    private ActivityResponse convertToResponse(ActivityLog activity) {
        return new ActivityResponse(
            activity.getId(),
            activity.getUserId(),
            activity.getUsername(),
            activity.getAction(),
            activity.getEntityType(),
            activity.getEntityId(),
            activity.getDescription(),
            activity.getModule(),
            activity.getSeverity(),
            activity.getTimestamp(),
            activity.getMetadata(),
            activity.getBeforeData(),
            activity.getAfterData()
        );
    }
}