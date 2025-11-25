package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.OneTimePermission;
import com.pos.inventsight.model.sql.PermissionType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.repository.sql.OneTimePermissionRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing one-time permissions
 */
@Service
public class OneTimePermissionService {
    
    @Autowired
    private OneTimePermissionRepository permissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyAuthorizationService companyAuthorizationService;
    
    @Value("${inventsight.permissions.default-expiry-hours:1}")
    private Integer defaultExpiryHours;
    
    /**
     * Grant a one-time permission to a user
     */
    @Transactional
    public OneTimePermission grantPermission(User grantedBy, User grantedTo, 
                                            PermissionType permissionType, Store store) {
        System.out.println("🔐 Granting " + permissionType + " permission from " + 
                         grantedBy.getEmail() + " to " + grantedTo.getEmail());
        
        // Verify granting user has GM+ privileges
        if (!hasManagerPrivileges(grantedBy)) {
            throw new RuntimeException("Only General Managers and above can grant permissions");
        }
        
        // Calculate expiration time (1 hour from now)
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(defaultExpiryHours);
        
        // Create permission
        OneTimePermission permission = new OneTimePermission(grantedTo, grantedBy, 
                                                            permissionType, expiresAt);
        permission.setStore(store);
        
        // Save permission
        OneTimePermission saved = permissionRepository.save(permission);
        
        System.out.println("✅ Permission granted successfully, expires at: " + expiresAt);
        return saved;
    }
    
    /**
     * Check if user has an active permission of the specified type
     */
    public boolean hasActivePermission(UUID userId, PermissionType permissionType) {
        LocalDateTime now = LocalDateTime.now();
        return permissionRepository.hasActivePermission(userId, permissionType, now);
    }
    
    /**
     * Get all active permissions for a user
     */
    public List<OneTimePermission> getActivePermissions(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return permissionRepository.findAllActivePermissionsForUser(userId, now);
    }
    
    /**
     * Get the first active permission of a specific type for a user
     */
    public Optional<OneTimePermission> getActivePermission(UUID userId, PermissionType permissionType) {
        LocalDateTime now = LocalDateTime.now();
        List<OneTimePermission> permissions = permissionRepository.findActivePermissions(
            userId, permissionType, now);
        
        return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
    }
    
    /**
     * Consume a one-time permission
     */
    @Transactional
    public void consumePermission(UUID permissionId) {
        System.out.println("🔓 Consuming permission: " + permissionId);
        
        OneTimePermission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found"));
        
        if (!permission.isValid()) {
            throw new RuntimeException("Permission is not valid or has already been used");
        }
        
        permission.consume();
        permissionRepository.save(permission);
        
        System.out.println("✅ Permission consumed successfully");
    }
    
    /**
     * Consume the first active permission of a specific type for a user
     */
    @Transactional
    public void consumePermission(UUID userId, PermissionType permissionType) {
        Optional<OneTimePermission> permission = getActivePermission(userId, permissionType);
        
        if (permission.isPresent()) {
            consumePermission(permission.get().getId());
        } else {
            throw new RuntimeException("No active " + permissionType + " permission found for user");
        }
    }
    
    /**
     * Expire old permissions - runs every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireOldPermissions() {
        System.out.println("⏰ Checking for permissions to expire");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OneTimePermission> permissionsToExpire = 
                permissionRepository.findPermissionsToExpire(now);
            
            if (!permissionsToExpire.isEmpty()) {
                System.out.println("📋 Found " + permissionsToExpire.size() + " permissions to expire");
                
                for (OneTimePermission permission : permissionsToExpire) {
                    permission.markAsExpired();
                    permissionRepository.save(permission);
                }
                
                System.out.println("✅ Expired " + permissionsToExpire.size() + " permissions");
            }
        } catch (Exception e) {
            System.out.println("❌ Error expiring permissions: " + e.getMessage());
        }
    }
    
    /**
     * Get all permissions granted to a user
     */
    public List<OneTimePermission> getPermissionsGrantedToUser(User user) {
        return permissionRepository.findByGrantedToUserOrderByGrantedAtDesc(user);
    }
    
    /**
     * Get all permissions granted by a user
     */
    public List<OneTimePermission> getPermissionsGrantedByUser(User user) {
        return permissionRepository.findByGrantedByUserOrderByGrantedAtDesc(user);
    }
    
    /**
     * Check if user has manager-level privileges (GM+)
     * Uses explicit role matching for robust authorization
     */
    private boolean hasManagerPrivileges(User user) {
        try {
            if (user.getRole() == null) {
                return false;
            }
            
            // Check for explicit manager-level roles
            UserRole role = user.getRole();
            return role == UserRole.MANAGER ||
                   role == UserRole.OWNER ||
                   role == UserRole.CO_OWNER ||
                   role == UserRole.ADMIN;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking user privileges: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if user can perform action (either has role or has permission)
     */
    public boolean canPerformAction(User user, PermissionType permissionType) {
        // Check if user has manager-level privileges
        if (hasManagerPrivileges(user)) {
            return true;
        }
        
        // Check if user has an active one-time permission
        return hasActivePermission(user.getId(), permissionType);
    }
}
