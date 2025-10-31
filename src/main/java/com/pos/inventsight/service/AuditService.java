package com.pos.inventsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.model.sql.AuditEvent;
import com.pos.inventsight.repository.AuditEventRepository;
import com.pos.inventsight.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing append-only audit events with optional hash chaining.
 * Provides tamper-evident audit trail for sensitive operations.
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditEventRepository auditEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log an audit event asynchronously
     */
    @Async
    @Transactional
    public void logAsync(String actor, Long actorId, String action, String entityType, String entityId, Object details) {
        try {
            log(actor, actorId, action, entityType, entityId, details);
        } catch (Exception e) {
            logger.error("Failed to log audit event asynchronously: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log an audit event synchronously
     */
    @Transactional
    public AuditEvent log(String actor, Long actorId, String action, String entityType, String entityId, Object details) {
        try {
            // Get current request context if available
            String ipAddress = null;
            String userAgent = null;
            
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }
            
            // Get tenant context
            String tenantId = TenantContext.getCurrentTenant();
            UUID tenantUuid = null;
            UUID companyUuid = null;
            
            if (tenantId != null && tenantId.startsWith("company_")) {
                try {
                    String uuidStr = tenantId.replace("company_", "").replace("_", "-");
                    companyUuid = UUID.fromString(uuidStr);
                    tenantUuid = companyUuid;
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid tenant UUID format: {}", tenantId);
                }
            }
            
            // Serialize details to JSON
            String detailsJson = null;
            if (details != null) {
                try {
                    detailsJson = objectMapper.writeValueAsString(details);
                } catch (Exception e) {
                    logger.warn("Failed to serialize audit details: {}", e.getMessage());
                    detailsJson = details.toString();
                }
            }
            
            // Build audit event
            AuditEvent event = AuditEvent.builder()
                    .actor(actor)
                    .actorId(actorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .tenantId(tenantUuid)
                    .companyId(companyUuid)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .detailsJson(detailsJson)
                    .build();
            
            // Compute hash with optional chaining
            computeHash(event);
            
            // Persist event
            AuditEvent savedEvent = auditEventRepository.save(event);
            logger.debug("Audit event logged: {} by {} (ID: {})", action, actor, savedEvent.getId());
            
            return savedEvent;
            
        } catch (Exception e) {
            logger.error("Failed to log audit event: {}", e.getMessage(), e);
            throw new RuntimeException("Audit logging failed", e);
        }
    }
    
    /**
     * Compute hash for audit event with optional chaining
     */
    private void computeHash(AuditEvent event) {
        try {
            // Get previous hash from latest event
            List<AuditEvent> latestEvents = auditEventRepository.findLatestEvent(Pageable.ofSize(1));
            String prevHash = latestEvents.isEmpty() ? null : latestEvents.get(0).getHash();
            
            // Build content to hash
            StringBuilder content = new StringBuilder();
            content.append(event.getEventAt());
            content.append(event.getActor());
            content.append(event.getAction());
            if (event.getEntityType() != null) content.append(event.getEntityType());
            if (event.getEntityId() != null) content.append(event.getEntityId());
            if (event.getTenantId() != null) content.append(event.getTenantId());
            if (event.getDetailsJson() != null) content.append(event.getDetailsJson());
            if (prevHash != null) content.append(prevHash);
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes);
            
            event.setPrevHash(prevHash);
            event.setHash(hash);
            
        } catch (Exception e) {
            logger.warn("Failed to compute audit event hash: {}", e.getMessage());
            // Continue without hash - audit event is still valuable
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Return first IP if multiple are present
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * Query audit events by tenant
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTenant(UUID tenantId, Pageable pageable) {
        return auditEventRepository.findByTenantIdOrderByEventAtDesc(tenantId, pageable);
    }
    
    /**
     * Query audit events by company
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByCompany(UUID companyId, Pageable pageable) {
        return auditEventRepository.findByCompanyIdOrderByEventAtDesc(companyId, pageable);
    }
    
    /**
     * Query audit events since a timestamp
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTenantSince(UUID tenantId, LocalDateTime since, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndEventAtAfter(tenantId, since, pageable);
    }
    
    /**
     * Query audit events by entity
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByEntity(String entityType, String entityId, Pageable pageable) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByEventAtDesc(entityType, entityId, pageable);
    }
}
