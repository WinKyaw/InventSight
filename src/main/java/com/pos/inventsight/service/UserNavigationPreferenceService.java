package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.UserNavigationPreference;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.UserNavigationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user navigation preferences.
 * Provides role-based default navigation tabs to prevent unauthorized access.
 */
@Service
@Transactional
public class UserNavigationPreferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserNavigationPreferenceService.class);
    
    // Role-based default tabs
    private static final List<String> GM_PLUS_TABS = Arrays.asList("items", "receipt", "team");
    private static final List<String> EMPLOYEE_TABS = Arrays.asList("items", "receipt", "calendar");
    
    @Autowired
    private UserNavigationPreferenceRepository navigationPreferenceRepository;
    
    /**
     * Get navigation preferences by user ID.
     * Creates default preferences based on role if they don't exist.
     * 
     * @param userId the user's UUID
     * @param userRole the user's role
     * @return the user navigation preferences
     */
    public UserNavigationPreference getNavigationPreferences(UUID userId, UserRole userRole) {
        logger.debug("Fetching navigation preferences for user: {}", userId);
        
        return navigationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.info("Creating default navigation preferences for user: {} with role: {}", userId, userRole);
                    return createDefaultPreferences(userId, userRole);
                });
    }
    
    /**
     * Update user's navigation preferences.
     * Validates that preferred tabs are within available tabs.
     * 
     * @param userId the user's UUID
     * @param preferredTabs list of preferred tab names
     * @return the updated navigation preferences
     * @throws IllegalArgumentException if preferred tabs contain unauthorized tabs
     */
    public UserNavigationPreference updateNavigationPreferences(UUID userId, List<String> preferredTabs) {
        logger.info("Updating navigation preferences for user: {}", userId);
        
        UserNavigationPreference preferences = navigationPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Navigation preferences not found for user: " + userId));
        
        // Validate that all preferred tabs are in available tabs
        List<String> availableTabs = preferences.getAvailableTabs();
        for (String tab : preferredTabs) {
            if (!availableTabs.contains(tab)) {
                // Provide specific error message for team tab access denial
                if ("team".equals(tab)) {
                    throw new IllegalArgumentException(
                        "Access denied: Team management requires General Manager level or above"
                    );
                }
                throw new IllegalArgumentException(
                    "Tab '" + tab + "' is not available for this user. " +
                    "Available tabs: " + availableTabs
                );
            }
        }
        
        preferences.setPreferredTabs(preferredTabs);
        // modifiedAt will be set automatically by @PreUpdate
        
        return navigationPreferenceRepository.save(preferences);
    }
    
    /**
     * Create default navigation preferences based on user role.
     * 
     * GM+ (OWNER, CO_OWNER, MANAGER, ADMIN): ["items", "receipt", "team"]
     * Employee: ["items", "receipt", "calendar"]
     * 
     * @param userId the user's UUID
     * @param userRole the user's role
     * @return the created navigation preferences
     */
    public UserNavigationPreference createDefaultPreferences(UUID userId, UserRole userRole) {
        logger.info("Creating default navigation preferences for user: {} with role: {}", userId, userRole);
        
        List<String> defaultTabs = getDefaultTabsForRole(userRole);
        List<String> availableTabs = getAvailableTabsForRole(userRole);
        
        UserNavigationPreference preferences = new UserNavigationPreference(userId, defaultTabs, availableTabs);
        // modifiedAt will be set automatically by @PrePersist
        
        return navigationPreferenceRepository.save(preferences);
    }
    
    /**
     * Check if role is GM+ level (has team access).
     * 
     * GM+ Roles: OWNER, CO_OWNER, MANAGER, ADMIN
     * Below GM: EMPLOYEE, CASHIER, CUSTOMER, MERCHANT, PARTNER, USER
     * 
     * @param role the user's role
     * @return true if role is GM+ level
     */
    private boolean isGMPlusRole(UserRole role) {
        return role == UserRole.OWNER 
            || role == UserRole.CO_OWNER 
            || role == UserRole.MANAGER 
            || role == UserRole.ADMIN;
    }
    
    /**
     * Get default tabs based on user role.
     * 
     * GM+ (OWNER, CO_OWNER, MANAGER, ADMIN): Can access team management
     * Below GM (EMPLOYEE, CASHIER, CUSTOMER, etc): Cannot access team
     * 
     * @param userRole the user's role
     * @return list of default tab names
     */
    private List<String> getDefaultTabsForRole(UserRole userRole) {
        if (userRole == null) {
            logger.warn("User role is null, defaulting to EMPLOYEE tabs");
            return EMPLOYEE_TABS;
        }
        
        if (isGMPlusRole(userRole)) {
            return GM_PLUS_TABS;
        }
        return EMPLOYEE_TABS;
    }
    
    /**
     * Get available tabs based on user role.
     * Below-GM users cannot access "team" tab at all.
     * 
     * @param role the user's role
     * @return list of available tab names
     */
    private List<String> getAvailableTabsForRole(UserRole role) {
        if (isGMPlusRole(role)) {
            // GM+ can access all tabs including team
            return Arrays.asList("items", "receipt", "team", "calendar", "dashboard", "reports");
        } else {
            // Below GM - NO TEAM ACCESS!
            return Arrays.asList("items", "receipt", "calendar", "dashboard");
        }
    }
    
    /**
     * Delete user navigation preferences.
     * 
     * @param userId the user's UUID
     */
    public void deleteNavigationPreferences(UUID userId) {
        logger.info("Deleting navigation preferences for user: {}", userId);
        
        navigationPreferenceRepository.findByUserId(userId)
                .ifPresent(navigationPreferenceRepository::delete);
    }
}
