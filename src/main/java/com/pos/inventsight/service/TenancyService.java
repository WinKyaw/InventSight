package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing multi-tenant context and role-based access control
 */
@Service
public class TenancyService {
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    /**
     * Get all stores that a user has access to
     */
    public List<Store> getUserStores(User user) {
        return userStoreRoleRepository.findStoresByUser(user);
    }
    
    /**
     * Get user's role in a specific store
     */
    public Optional<UserRole> getUserRoleInStore(User user, Store store) {
        return userStoreRoleRepository.findRoleByUserAndStore(user, store);
    }
    
    /**
     * Check if user has access to a store
     */
    public boolean hasStoreAccess(User user, Store store) {
        return userStoreRoleRepository.existsByUserAndStoreAndIsActiveTrue(user, store);
    }
    
    /**
     * Check if user has owner/co-owner access in any store
     */
    public boolean hasOwnerAccess(User user) {
        List<UserStoreRole> ownerRoles = userStoreRoleRepository.findOwnerRolesByUser(user);
        return !ownerRoles.isEmpty();
    }
    
    /**
     * Check if user has admin access (owner, co-owner, or manager) in a store
     */
    public boolean hasAdminAccess(User user, Store store) {
        Optional<UserRole> role = getUserRoleInStore(user, store);
        if (role.isPresent()) {
            UserRole userRole = role.get();
            return userRole == UserRole.OWNER || 
                   userRole == UserRole.CO_OWNER || 
                   userRole == UserRole.MANAGER;
        }
        return false;
    }
    
    /**
     * Check if user can view original prices (owner/co-owner only)
     */
    public boolean canViewOriginalPrices(User user, Store store) {
        Optional<UserRole> role = getUserRoleInStore(user, store);
        if (role.isPresent()) {
            UserRole userRole = role.get();
            return userRole == UserRole.OWNER || userRole == UserRole.CO_OWNER;
        }
        return false;
    }
    
    /**
     * Get user's active role mappings
     */
    public List<UserStoreRole> getUserActiveRoles(User user) {
        return userStoreRoleRepository.findByUserAndIsActiveTrue(user);
    }
    
    /**
     * Get all users in a store with their roles
     */
    public List<UserStoreRole> getStoreUsers(Store store) {
        return userStoreRoleRepository.findByStoreAndIsActiveTrue(store);
    }
    
    /**
     * Count active users in a store
     */
    public long countStoreUsers(Store store) {
        return userStoreRoleRepository.countActiveUsersByStore(store);
    }
    
    /**
     * Count users with specific role in a store
     */
    public long countStoreUsersByRole(Store store, UserRole role) {
        return userStoreRoleRepository.countActiveUsersByStoreAndRole(store, role);
    }
    
    /**
     * Check if user can approve discounts (manager level and above)
     */
    public boolean canApproveDiscounts(User user, Store store) {
        return hasAdminAccess(user, store);
    }
    
    /**
     * Check if user can set owner pricing (owner/co-owner only)
     */
    public boolean canSetOwnerPricing(User user, Store store) {
        return canViewOriginalPrices(user, store);
    }
    
    /**
     * Assign role to user in store
     */
    public UserStoreRole assignUserToStore(User user, Store store, UserRole role, String assignedBy) {
        // Check if mapping already exists
        Optional<UserStoreRole> existing = userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(user, store);
        if (existing.isPresent()) {
            // Update existing role
            UserStoreRole userStoreRole = existing.get();
            userStoreRole.setRole(role);
            return userStoreRoleRepository.save(userStoreRole);
        } else {
            // Create new mapping
            UserStoreRole userStoreRole = new UserStoreRole(user, store, role, assignedBy);
            return userStoreRoleRepository.save(userStoreRole);
        }
    }
    
    /**
     * Revoke user access to store
     */
    public void revokeUserStoreAccess(User user, Store store, String revokedBy) {
        Optional<UserStoreRole> userStoreRole = userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(user, store);
        if (userStoreRole.isPresent()) {
            userStoreRole.get().revokeRole(revokedBy);
            userStoreRoleRepository.save(userStoreRole.get());
        }
    }
}