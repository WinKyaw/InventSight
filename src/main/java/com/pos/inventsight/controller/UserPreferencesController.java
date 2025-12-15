package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserPreferences;
import com.pos.inventsight.model.sql.UserNavigationPreference;
import com.pos.inventsight.service.UserPreferencesService;
import com.pos.inventsight.service.UserNavigationPreferenceService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user preferences including language, theme, and favorite tabs.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Preferences", description = "User preference management")
public class UserPreferencesController {
    
    @Autowired
    private UserPreferencesService userPreferencesService;
    
    @Autowired
    private UserNavigationPreferenceService navigationPreferenceService;
    
    @Autowired
    private UserService userService;
    
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
     * 
     * @param authentication the authenticated user
     * @return the user's navigation preferences
     */
    @GetMapping("/me/navigation-preferences")
    @Operation(summary = "Get navigation preferences", description = "Retrieve the authenticated user's navigation tab preferences")
    public ResponseEntity<GenericApiResponse<NavigationPreferencesResponse>> getNavigationPreferences(
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            UserNavigationPreference preferences = navigationPreferenceService.getNavigationPreferences(
                    user.getId(), user.getRole());
            NavigationPreferencesResponse response = new NavigationPreferencesResponse(preferences);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Navigation preferences retrieved successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update current user's navigation preferences.
     * Validates that preferred tabs are within available tabs based on role.
     * 
     * @param authentication the authenticated user
     * @param request the navigation preferences request
     * @return the updated navigation preferences
     */
    @PostMapping("/me/navigation-preferences")
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
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
}
