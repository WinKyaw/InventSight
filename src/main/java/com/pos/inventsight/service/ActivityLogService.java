package com.pos.inventsight.service;

import com.pos.inventsight.model.nosql.ActivityLog;
import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ActivityLogService {
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    // Create Activity Log
    public void logActivity(String userId, String username, String action, String entityType, String description) {
        ActivityLog log = new ActivityLog(userId, username != null ? username : "WinKyaw", action, entityType, description);
        log.setTimestamp(LocalDateTime.now());
        
        // Set module based on entity type
        log.setModule(getModuleFromEntityType(entityType));
        
        activityLogRepository.save(log);
        
        // Console logging for development
        System.out.println(String.format("📝 Activity Log: [%s] %s performed %s on %s - %s", 
            LocalDateTime.now().toString(), username != null ? username : "WinKyaw", action, entityType, description));
    }
    
    public void logActivityWithMetadata(String userId, String username, String action, String entityType, 
                                      String description, Map<String, Object> metadata) {
        ActivityLog log = new ActivityLog(userId, username != null ? username : "WinKyaw", action, entityType, description);
        log.setMetadata(metadata);
        log.setModule(getModuleFromEntityType(entityType));
        log.setTimestamp(LocalDateTime.now());
        
        activityLogRepository.save(log);
    }
    
    public void logActivityWithDataChange(String userId, String username, String action, String entityType,
                                        String entityId, String description, 
                                        Map<String, Object> beforeData, Map<String, Object> afterData) {
        ActivityLog log = new ActivityLog(userId, username != null ? username : "WinKyaw", action, entityType, description);
        log.setEntityId(entityId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setModule(getModuleFromEntityType(entityType));
        log.setTimestamp(LocalDateTime.now());
        
        activityLogRepository.save(log);
    }
    
    // Query Methods
    public List<ActivityLog> getActivityLogsByUser(String userId) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    public List<ActivityLog> getActivityLogsByUsername(String username) {
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
    
    public List<ActivityLog> getActivityLogsByAction(String action) {
        return activityLogRepository.findByActionOrderByTimestampDesc(action);
    }
    
    public List<ActivityLog> getActivityLogsByEntityType(String entityType) {
        return activityLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }
    
    public List<ActivityLog> getActivityLogsByModule(String module) {
        return activityLogRepository.findByModuleOrderByTimestampDesc(module);
    }
    
    public List<ActivityLog> getActivityLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return activityLogRepository.findByTimestampBetween(start, end);
    }
    
    public Page<ActivityLog> getAllActivityLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    public List<ActivityLog> getTodayActivityLogs() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getActivityLogsByDateRange(startOfDay, endOfDay);
    }
    
    // Statistics
    public long getUserActivityCount(String userId) {
        return activityLogRepository.countByUserId(userId);
    }
    
    public long getUsernameActivityCount(String username) {
        return activityLogRepository.countByUsername(username);
    }
    
    // Current system status
    public ActivityLog logSystemStatus() {
        ActivityLog statusLog = new ActivityLog();
        statusLog.setUserId("SYSTEM");
        statusLog.setUsername("WinKyaw");
        statusLog.setAction("SYSTEM_STATUS");
        statusLog.setEntityType("SYSTEM");
        statusLog.setDescription("System status check - Current DateTime (UTC): 2025-08-26 08:47:36, Current User: WinKyaw");
        statusLog.setTimestamp(LocalDateTime.now());
        statusLog.setSeverity("INFO");
        statusLog.setModule("SYSTEM");
        
        return activityLogRepository.save(statusLog);
    }
    
    // Helper method
    private String getModuleFromEntityType(String entityType) {
        if (entityType == null) return "SYSTEM";
        
        switch (entityType.toUpperCase()) {
            case "PRODUCT":
                return "INVENTORY";
            case "SALE":
            case "TRANSACTION":
                return "SALES";
            case "EMPLOYEE":
                return "EMPLOYEE";
            case "USER":
                return "AUTH";
            default:
                return "SYSTEM";
        }
    }
}