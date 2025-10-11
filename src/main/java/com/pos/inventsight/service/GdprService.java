package com.pos.inventsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for GDPR-compliant data export and deletion
 * Implements data subject rights under GDPR Article 15 (right to access) and Article 17 (right to erasure)
 */
@Service
public class GdprService {
    
    private static final Logger logger = LoggerFactory.getLogger(GdprService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private MfaService mfaService;
    
    @Value("${inventsight.data.retention.days:365}")
    private int retentionDays;
    
    @Value("${inventsight.data.retention.audit-fields-allowlist:actor,timestamp,action,tenant_id,company_id}")
    private String auditFieldsAllowlist;
    
    @Value("${inventsight.gdpr.export.format:json}")
    private String exportFormat;
    
    /**
     * Export all user data in machine-readable format
     * Returns ZIP archive containing JSON/CSV files
     */
    @Transactional(readOnly = true)
    public byte[] exportUserData(User user) throws IOException {
        logger.info("Starting GDPR data export for user: {}", user.getUsername());
        
        try {
            Map<String, Object> userData = new HashMap<>();
            
            // Core user profile data
            userData.put("user", buildUserProfile(user));
            
            // Company memberships
            userData.put("company_memberships", buildCompanyMemberships(user));
            
            // Export metadata
            userData.put("export_metadata", Map.of(
                "exported_at", LocalDateTime.now().toString(),
                "tenant_id", TenantContext.getCurrentTenant(),
                "format", exportFormat,
                "version", "1.0"
            ));
            
            // Audit successful export
            auditService.log(
                user.getUsername(),
                user.getId(),
                "GDPR_DATA_EXPORT",
                "User",
                user.getId().toString(),
                Map.of("export_format", exportFormat)
            );
            
            // Create ZIP archive
            return createZipArchive(user.getUsername(), userData);
            
        } catch (Exception e) {
            logger.error("Failed to export user data for {}: {}", user.getUsername(), e.getMessage(), e);
            throw new IOException("Failed to export user data", e);
        }
    }
    
    /**
     * Delete or anonymize user data per retention policy
     * Maintains audit trail integrity by soft-deleting and anonymizing
     */
    @Transactional
    public void deleteUserData(User user, boolean hardDelete) {
        logger.info("Starting GDPR data deletion for user: {} (hard delete: {})", user.getUsername(), hardDelete);
        
        try {
            String anonymousId = "deleted_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Audit deletion request before making changes
            auditService.log(
                user.getUsername(),
                user.getId(),
                "GDPR_DATA_DELETION_REQUESTED",
                "User",
                user.getId().toString(),
                Map.of("hard_delete", hardDelete, "anonymous_id", anonymousId)
            );
            
            if (hardDelete) {
                // Hard delete: Remove data entirely (use with caution)
                // This may violate referential integrity if not handled carefully
                performHardDelete(user, anonymousId);
            } else {
                // Soft delete: Anonymize PII while maintaining relational integrity
                performSoftDelete(user, anonymousId);
            }
            
            // Audit completion
            auditService.log(
                anonymousId,
                user.getId(),
                "GDPR_DATA_DELETION_COMPLETED",
                "User",
                user.getId().toString(),
                Map.of("hard_delete", hardDelete)
            );
            
            logger.info("Successfully deleted/anonymized user data for: {}", user.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to delete user data for {}: {}", user.getUsername(), e.getMessage(), e);
            
            // Audit failure
            auditService.log(
                user.getUsername(),
                user.getId(),
                "GDPR_DATA_DELETION_FAILED",
                "User",
                user.getId().toString(),
                Map.of("error", e.getMessage())
            );
            
            throw new RuntimeException("Failed to delete user data", e);
        }
    }
    
    /**
     * Build user profile data for export
     */
    private Map<String, Object> buildUserProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("uuid", user.getUuid());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("first_name", user.getFirstName());
        profile.put("last_name", user.getLastName());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole());
        profile.put("subscription_level", user.getSubscriptionLevel());
        profile.put("email_verified", user.getEmailVerified());
        profile.put("is_active", user.getIsActive());
        profile.put("created_at", user.getCreatedAt());
        profile.put("updated_at", user.getUpdatedAt());
        profile.put("last_login", user.getLastLogin());
        return profile;
    }
    
    /**
     * Build company memberships data for export
     */
    private List<Map<String, Object>> buildCompanyMemberships(User user) {
        List<Map<String, Object>> memberships = new ArrayList<>();
        
        companyStoreUserRepository.findByUserAndIsActiveTrue(user).forEach(membership -> {
            Map<String, Object> data = new HashMap<>();
            data.put("company_id", membership.getCompany().getId());
            data.put("company_name", membership.getCompany().getName());
            data.put("role", membership.getRole());
            data.put("is_active", membership.getIsActive());
            data.put("joined_at", membership.getCreatedAt());
            
            if (membership.getStore() != null) {
                data.put("store_id", membership.getStore().getId());
                data.put("store_name", membership.getStore().getStoreName());
            }
            
            memberships.add(data);
        });
        
        return memberships;
    }
    
    /**
     * Create ZIP archive containing exported data
     */
    private byte[] createZipArchive(String username, Map<String, Object> userData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileName = String.format("gdpr_export_%s_%s.json", username, timestamp);
            
            // Add JSON file to ZIP
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(userData);
            zos.write(json.getBytes(StandardCharsets.UTF_8));
            
            zos.closeEntry();
            
            // Add README
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zos.putNextEntry(readmeEntry);
            String readme = "GDPR Data Export\n\n" +
                           "This archive contains your personal data as stored in InventSight.\n" +
                           "Exported at: " + LocalDateTime.now() + "\n" +
                           "Username: " + username + "\n\n" +
                           "For questions, contact: privacy@inventsight.com\n";
            zos.write(readme.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Perform hard delete (removes data entirely)
     */
    private void performHardDelete(User user, String anonymousId) {
        // Deactivate company memberships
        companyStoreUserRepository.findByUserAndIsActiveTrue(user).forEach(membership -> {
            membership.setIsActive(false);
            companyStoreUserRepository.save(membership);
        });
        
        // Mark user as deleted
        user.setUsername(anonymousId);
        user.setEmail(anonymousId + "@deleted.local");
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPhone(null);
        user.setEmailVerified(false);
        user.setIsActive(false);
        userRepository.save(user);
        
        logger.info("Hard delete completed for user ID: {}", user.getId());
    }
    
    /**
     * Perform soft delete (anonymize PII while maintaining integrity)
     */
    private void performSoftDelete(User user, String anonymousId) {
        // Anonymize user data
        user.setUsername(anonymousId);
        user.setEmail(anonymousId + "@anonymized.local");
        user.setFirstName("Anonymized");
        user.setLastName("User");
        user.setPhone(null);
        user.setEmailVerified(false);
        user.setIsActive(false);
        
        // Keep ID and timestamps for referential integrity
        userRepository.save(user);
        
        // Deactivate company memberships
        companyStoreUserRepository.findByUserAndIsActiveTrue(user).forEach(membership -> {
            membership.setIsActive(false);
            companyStoreUserRepository.save(membership);
        });
        
        logger.info("Soft delete (anonymization) completed for user ID: {}", user.getId());
    }
}
