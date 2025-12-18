package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRoleRepository;
import com.pos.inventsight.repository.sql.UserNavigationPreferenceRepository;
import com.pos.inventsight.service.CompanyService;
import com.pos.inventsight.service.UserPreferencesService;
import com.pos.inventsight.service.UserNavigationPreferenceService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing user preferences including language, theme, and favorite tabs.
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Preferences", description = "User preference management")
public class UserPreferencesController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesController.class);
    
    @Autowired
    private UserPreferencesService userPreferencesService;
    
    @Autowired
    private UserNavigationPreferenceService navigationPreferenceService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private CompanyStoreUserRoleRepository companyStoreUserRoleRepository;
    
    @Autowired
    private UserNavigationPreferenceRepository navigationPreferenceRepository;
    
    /**
     * Get current user's preferences.
     * Authenticated endpoint.
     * 
     * @param authentication the authenticated user
     * @return the user's preferences
     */
    @GetMapping("/me/preferences")
    @Operation(summary = "Get user preferences", description = "Retrieve the authenticated user's preferences")
    public ResponseEntity<GenericApiResponse<UserPreferencesResponse>> getUserPreferences(
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserPreferences preferences = userPreferencesService.getUserPreferences(user.getId());
            UserPreferencesResponse response = new UserPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Preferences retrieved successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update user's preferred language.
     * 
     * @param authentication the authenticated user
     * @param request the language preference request
     * @return the updated preferences
     */
    @PutMapping("/me/preferences/language")
    @Operation(summary = "Update language preference", description = "Update the authenticated user's preferred language")
    public ResponseEntity<GenericApiResponse<UserPreferencesResponse>> updateLanguagePreference(
            Authentication authentication,
            @Valid @RequestBody UserPreferencesRequest request) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserPreferences preferences = userPreferencesService.updateLanguagePreference(
                    user.getId(), request.getLanguageCode());
            UserPreferencesResponse response = new UserPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Language preference updated successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update user's favorite tabs.
     * 
     * @param authentication the authenticated user
     * @param request the favorite tabs request
     * @return the updated preferences
     */
    @PutMapping("/me/preferences/favorite-tabs")
    @Operation(summary = "Update favorite tabs", description = "Update the authenticated user's favorite tabs configuration")
    public ResponseEntity<GenericApiResponse<UserPreferencesResponse>> updateFavoriteTabs(
            Authentication authentication,
            @Valid @RequestBody UserPreferencesRequest request) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserPreferences preferences = userPreferencesService.updateFavoriteTabs(
                    user.getId(), request.getTabs());
            UserPreferencesResponse response = new UserPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Favorite tabs updated successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update user's theme preference.
     * 
     * @param authentication the authenticated user
     * @param request the theme preference request
     * @return the updated preferences
     */
    @PutMapping("/me/preferences/theme")
    @Operation(summary = "Update theme preference", description = "Update the authenticated user's theme preference (light/dark)")
    public ResponseEntity<GenericApiResponse<UserPreferencesResponse>> updateTheme(
            Authentication authentication,
            @Valid @RequestBody UserPreferencesRequest request) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserPreferences preferences = userPreferencesService.updateTheme(
                    user.getId(), request.getTheme());
            UserPreferencesResponse response = new UserPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Theme preference updated successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get current user's navigation preferences.
     * Returns role-based navigation tabs.
     * Supports both /me/navigation-preferences and /navigation-preferences for backward compatibility.
     * 
     * @param authentication the authenticated user
     * @return the user's navigation preferences
     */
    @GetMapping({"/me/navigation-preferences", "/navigation-preferences"})
    @Operation(summary = "Get navigation preferences", description = "Retrieve the authenticated user's navigation tab preferences")
    public ResponseEntity<GenericApiResponse<NavigationPreferencesResponse>> getNavigationPreferences(
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // ✅ Get active company role
            UserRole activeRole = getActiveRole(user);
            
            logger.info("📱 Getting navigation preferences for {} with active role: {}", username, activeRole);
            
            UserNavigationPreference preferences = navigationPreferenceService.getNavigationPreferences(
                    user.getId(), 
                    activeRole  // ✅ Use active role
            );
            NavigationPreferencesResponse response = new NavigationPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(
                true, 
                "Navigation preferences retrieved successfully", 
                response
            ));
                    
        } catch (Exception e) {
            logger.error("❌ Error fetching navigation preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update current user's navigation preferences.
     * Validates that preferred tabs are within available tabs based on role.
     * Supports both /me/navigation-preferences and /navigation-preferences for backward compatibility.
     * 
     * @param authentication the authenticated user
     * @param request the navigation preferences request
     * @return the updated navigation preferences
     */
    @PostMapping({"/me/navigation-preferences", "/navigation-preferences"})
    @Operation(summary = "Update navigation preferences", description = "Update the authenticated user's navigation tab preferences")
    public ResponseEntity<GenericApiResponse<NavigationPreferencesResponse>> updateNavigationPreferences(
            Authentication authentication,
            @Valid @RequestBody NavigationPreferencesRequest request) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserNavigationPreference preferences = navigationPreferenceService.updateNavigationPreferences(
                    user.getId(), request.getPreferredTabs());
            NavigationPreferencesResponse response = new NavigationPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Navigation preferences updated successfully", response));
                    
        } catch (IllegalArgumentException e) {
            logger.error("Invalid navigation preferences update", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating navigation preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get available navigation tabs for current user based on role.
     * This tells the frontend which tabs to show in the Menu.
     * 
     * @param authentication the authenticated user
     * @return list of available tabs
     */
    @GetMapping({"/me/navigation-preferences/available", "/navigation-preferences/available"})
    @Operation(
        summary = "Get available navigation tabs", 
        description = "Get list of tabs user can access based on their role (for Menu filtering)"
    )
    public ResponseEntity<GenericApiResponse<List<String>>> getAvailableTabs(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // ✅ Get active company role
            UserRole activeRole = getActiveRole(user);
            
            UserNavigationPreference preferences = navigationPreferenceService.getNavigationPreferences(
                    user.getId(), activeRole);
            
            return ResponseEntity.ok(new GenericApiResponse<>(
                true, 
                "Available tabs retrieved successfully", 
                preferences.getAvailableTabs()
            ));
                    
        } catch (Exception e) {
            logger.error("Error fetching available tabs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Clear navigation preferences cache for current user.
     * Forces recreation of navigation preferences on next request.
     * 
     * @param authentication the authenticated user
     * @return success response
     */
    @DeleteMapping({"/me/navigation-preferences/cache", "/navigation-preferences/cache"})
    @Operation(summary = "Clear navigation cache", description = "Clear navigation preferences cache to force refresh")
    public ResponseEntity<GenericApiResponse<Void>> clearNavigationCache(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Delete existing preferences to force recreation
            navigationPreferenceRepository.deleteByUserId(user.getId());
            
            logger.info("🗑️ Cleared navigation cache for user: {}", username);
            
            return ResponseEntity.ok(new GenericApiResponse<>(
                true, 
                "Navigation cache cleared successfully", 
                null
            ));
            
        } catch (Exception e) {
            logger.error("❌ Error clearing navigation cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get user's active role from company_store_user_roles, fallback to global role.
     * Converts CompanyRole to UserRole for navigation system compatibility.
     * 
     * @param user the user
     * @return the active UserRole
     */
    private UserRole getActiveRole(User user) {
        try {
            // Get user's companies
            List<Company> companies = companyStoreUserRepository.findCompaniesByUser(user);
            
            if (!companies.isEmpty()) {
                // Use first company as primary (companies are typically ordered by creation date)
                // TODO: Consider adding explicit primary company flag or user preference
                Company primaryCompany = companies.get(0);
                
                List<CompanyStoreUserRole> roles = companyStoreUserRoleRepository
                    .findByUserAndCompanyAndIsActiveTrue(user, primaryCompany);
                
                // Filter out expired roles (extract current time to avoid repeated system calls)
                LocalDateTime now = LocalDateTime.now();
                List<CompanyStoreUserRole> validRoles = roles.stream()
                    .filter(role -> role.getExpiresAt() == null || now.isBefore(role.getExpiresAt()))
                    .toList();
                
                if (!validRoles.isEmpty()) {
                    // Return highest priority role (first role is highest priority based on CompanyRole enum order)
                    CompanyRole companyRole = validRoles.get(0).getRole();
                    UserRole mappedRole = mapCompanyRoleToUserRole(companyRole);
                    logger.info("✅ Using company role: {} → UserRole: {}", companyRole, mappedRole);
                    return mappedRole;
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not get company role, using global role: {}", e.getMessage());
        }
        
        // Fallback to global role
        logger.info("ℹ️ Using global role: {}", user.getRole());
        return user.getRole();
    }
    
    /**
     * Map CompanyRole to equivalent UserRole for navigation system.
     * 
     * @param companyRole the company role
     * @return the equivalent UserRole
     */
    private UserRole mapCompanyRoleToUserRole(CompanyRole companyRole) {
        if (companyRole == null) {
            return UserRole.EMPLOYEE;
        }
        
        switch (companyRole) {
            case FOUNDER:
                return UserRole.FOUNDER;
            case CEO:
                return UserRole.OWNER;
            case GENERAL_MANAGER:
                return UserRole.MANAGER;
            case STORE_MANAGER:
                return UserRole.MANAGER;
            case EMPLOYEE:
                return UserRole.EMPLOYEE;
            default:
                logger.warn("Unknown CompanyRole: {}, defaulting to EMPLOYEE", companyRole);
                return UserRole.EMPLOYEE;
        }
    }
}
