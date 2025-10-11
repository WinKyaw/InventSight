package com.pos.inventsight.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.model.sql.SyncChange;
import com.pos.inventsight.repository.sql.SyncChangeRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Service for managing sync changes for offline sync support.
 * Provides change feed functionality for incremental synchronization.
 */
@Service
public class SyncChangeService {
    
    private static final Logger logger = LoggerFactory.getLogger(SyncChangeService.class);
    
    private final SyncChangeRepository syncChangeRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${inventsight.sync.change-feed.enabled:true}")
    private boolean changeFeedEnabled;
    
    @Value("${inventsight.sync.change-feed.page-size:100}")
    private int defaultPageSize;
    
    public SyncChangeService(SyncChangeRepository syncChangeRepository, ObjectMapper objectMapper) {
        this.syncChangeRepository = syncChangeRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Record a change event for sync tracking
     * @param entityType Type of entity (e.g., "Product", "Sale")
     * @param entityId ID of the entity
     * @param operation Operation type (INSERT, UPDATE, DELETE)
     * @param changeData JSON snapshot of the change
     */
    @Transactional
    public void recordChange(String entityType, String entityId, String operation, Object changeData) {
        if (!changeFeedEnabled) {
            logger.debug("Change feed disabled, skipping change recording");
            return;
        }
        
        try {
            UUID tenantId = getCurrentTenantId();
            if (tenantId == null) {
                logger.debug("No tenant context, skipping change recording");
                return;
            }
            
            SyncChange syncChange = new SyncChange();
            syncChange.setTenantId(tenantId);
            syncChange.setCompanyId(tenantId);
            syncChange.setEntityType(entityType);
            syncChange.setEntityId(entityId);
            syncChange.setOperation(operation);
            syncChange.setChangedAt(LocalDateTime.now());
            
            // Serialize change data to JSON
            if (changeData != null) {
                try {
                    String jsonData = objectMapper.writeValueAsString(changeData);
                    syncChange.setChangeData(jsonData);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to serialize change data: {}", e.getMessage());
                    syncChange.setChangeData("{}");
                }
            }
            
            syncChangeRepository.save(syncChange);
            logger.debug("Recorded sync change: {} {} for entity {} {}", 
                        operation, entityType, entityId, tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to record sync change: {}", e.getMessage(), e);
            // Don't fail the main operation if sync recording fails
        }
    }
    
    /**
     * Get changes since a watermark (timestamp or opaque string)
     * @param tenantId Tenant ID
     * @param watermark Watermark (ISO-8601 timestamp or opaque token)
     * @param limit Maximum number of changes to return (max 500)
     * @return Page of sync changes
     */
    @Transactional(readOnly = true)
    public Page<SyncChange> getChangesSince(UUID tenantId, String watermark, Integer limit) {
        int pageSize = Math.min(limit != null ? limit : defaultPageSize, 500);
        Pageable pageable = PageRequest.of(0, pageSize);
        
        if (watermark != null && !watermark.trim().isEmpty()) {
            try {
                // Try to parse as ISO-8601 timestamp
                LocalDateTime since = LocalDateTime.parse(watermark, DateTimeFormatter.ISO_DATE_TIME);
                return syncChangeRepository.findByTenantIdAndChangedAtAfterOrderByChangedAtAsc(
                    tenantId, since, pageable);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid watermark format: {}, returning recent changes", watermark);
            }
        }
        
        // If no watermark or invalid, return most recent changes
        return syncChangeRepository.findByTenantIdOrderByChangedAtDesc(tenantId, pageable);
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
