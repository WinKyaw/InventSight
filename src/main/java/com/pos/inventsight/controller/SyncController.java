package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.SyncChange;
import com.pos.inventsight.service.SyncChangeService;
import com.pos.inventsight.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for sync operations, providing change feed for offline sync support.
 */
@RestController
@RequestMapping("/sync")
public class SyncController {
    
    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);
    
    private final SyncChangeService syncChangeService;
    
    public SyncController(SyncChangeService syncChangeService) {
        this.syncChangeService = syncChangeService;
    }
    
    /**
     * Get sync changes since a watermark
     * 
     * @param since Watermark (ISO-8601 timestamp) - optional
     * @param limit Maximum number of changes to return (max 500, default 100)
     * @return List of changes with pagination metadata
     */
    @GetMapping("/changes")
    @PreAuthorize("hasAnyRole('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<?> getChanges(
            @RequestParam(required = false) String since,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        
        try {
            // Get tenant ID from context
            UUID tenantId = getCurrentTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No tenant context available"));
            }
            
            // Validate limit
            if (limit != null && limit > 500) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Limit cannot exceed 500"));
            }
            
            // Get changes
            Page<SyncChange> changesPage = syncChangeService.getChangesSince(tenantId, since, limit);
            
            // Build response
            List<Map<String, Object>> changes = new ArrayList<>();
            for (SyncChange change : changesPage.getContent()) {
                Map<String, Object> changeMap = new HashMap<>();
                changeMap.put("id", change.getId().toString());
                changeMap.put("entity_type", change.getEntityType());
                changeMap.put("entity_id", change.getEntityId());
                changeMap.put("operation", change.getOperation());
                changeMap.put("changed_at", change.getChangedAt().format(DateTimeFormatter.ISO_DATE_TIME));
                changeMap.put("change_data", change.getChangeData());
                changeMap.put("version", change.getVersion());
                changes.add(changeMap);
            }
            
            // Determine next watermark
            String nextWatermark = null;
            boolean hasMore = changesPage.hasNext();
            if (!changesPage.isEmpty()) {
                SyncChange lastChange = changesPage.getContent().get(changesPage.getContent().size() - 1);
                nextWatermark = lastChange.getChangedAt().format(DateTimeFormatter.ISO_DATE_TIME);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("changes", changes);
            response.put("count", changes.size());
            response.put("has_more", hasMore);
            if (nextWatermark != null) {
                response.put("next_watermark", nextWatermark);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving sync changes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve sync changes"));
        }
    }
    
    /**
     * Extract tenant ID from current context
     * @return Tenant UUID or null if not found
     */
    private UUID getCurrentTenantId() {
        String tenantContext = TenantContext.getCurrentTenant();
        if (tenantContext != null && tenantContext.startsWith("company_")) {
            try {
                String uuidStr = tenantContext.substring(8).replace("_", "-");
                return UUID.fromString(uuidStr);
            } catch (Exception e) {
                logger.debug("Failed to parse tenant UUID from context: {}", tenantContext);
            }
        }
        return null;
    }
}
