package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.IdempotencyKey;
import com.pos.inventsight.repository.sql.IdempotencyKeyRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing idempotency keys to ensure duplicate requests are handled correctly.
 * Provides request hashing, key storage/lookup, and response replay capabilities.
 */
@Service
public class IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    
    @Value("${inventsight.sync.idempotency.ttl-hours:24}")
    private int ttlHours;
    
    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }
    
    /**
     * Compute SHA-256 hash of request (method + path + body)
     * @param method HTTP method
     * @param path Request path
     * @param body Request body (can be null)
     * @return SHA-256 hash as hex string
     */
    public String computeRequestHash(String method, String path, String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String requestData = method + ":" + path + ":" + (body != null ? body : "");
            byte[] hash = digest.digest(requestData.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to compute request hash: {}", e.getMessage());
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }
    
    /**
     * Find existing idempotency key for the given key and tenant
     * @param key Idempotency key
     * @param tenantId Tenant ID
     * @return Optional containing the IdempotencyKey if found
     */
    public Optional<IdempotencyKey> findIdempotencyKey(String key, UUID tenantId) {
        return idempotencyKeyRepository.findByIdempotencyKeyAndTenantId(key, tenantId);
    }
    
    /**
     * Store a new idempotency key with response details
     * @param key Idempotency key
     * @param tenantId Tenant ID
     * @param companyId Company ID (optional)
     * @param endpoint Request endpoint
     * @param requestHash Hash of the request
     * @param responseStatus HTTP response status
     * @param responseBody Response body
     * @return The stored IdempotencyKey
     */
    @Transactional
    public IdempotencyKey storeIdempotencyKey(String key, UUID tenantId, UUID companyId,
                                               String endpoint, String requestHash,
                                               int responseStatus, String responseBody) {
        IdempotencyKey idempotencyKey = new IdempotencyKey();
        idempotencyKey.setIdempotencyKey(key);
        idempotencyKey.setTenantId(tenantId);
        idempotencyKey.setCompanyId(companyId);
        idempotencyKey.setEndpoint(endpoint);
        idempotencyKey.setRequestHash(requestHash);
        idempotencyKey.setResponseStatus(responseStatus);
        idempotencyKey.setResponseBody(responseBody);
        idempotencyKey.setCreatedAt(LocalDateTime.now());
        idempotencyKey.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        
        logger.debug("Storing idempotency key: {} for tenant: {}", key, tenantId);
        return idempotencyKeyRepository.save(idempotencyKey);
    }
    
    /**
     * Clean up expired idempotency keys
     * Scheduled to run every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredKeys() {
        try {
            int deletedCount = idempotencyKeyRepository.deleteExpiredKeys(LocalDateTime.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired idempotency keys", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired idempotency keys: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Extract tenant ID from current context or parse from tenant context string
     * @return Tenant UUID or null if not found
     */
    public UUID getCurrentTenantId() {
        String tenantContext = TenantContext.getCurrentTenant();
        if (tenantContext != null && tenantContext.startsWith("company_")) {
            try {
                // Extract UUID from "company_<uuid_with_underscores>"
                String uuidStr = tenantContext.substring(8).replace("_", "-");
                return UUID.fromString(uuidStr);
            } catch (Exception e) {
                logger.debug("Failed to parse tenant UUID from context: {}", tenantContext);
            }
        }
        return null;
    }
}
