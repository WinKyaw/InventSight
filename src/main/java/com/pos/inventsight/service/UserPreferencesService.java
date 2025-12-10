package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.UserPreferences;
import com.pos.inventsight.repository.sql.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user preferences including language, theme, and favorite tabs.
 */
@Service
@Transactional
public class UserPreferencesService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    /**
     * Get user preferences by user ID.
     * Creates default preferences if they don't exist.
     * 
     * @param userId the user's UUID
     * @return the user preferences
     */
    public UserPreferences getUserPreferences(UUID userId) {
        logger.debug("Fetching preferences for user: {}", userId);
        
        return userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.info("Creating default preferences for user: {}", userId);
                    return createDefaultPreferences(userId);
                });
    }
    
    /**
     * Update user's preferred language.
     * 
     * @param userId the user's UUID
     * @param languageCode the language code
     * @return the updated user preferences
     */
    public UserPreferences updateLanguagePreference(UUID userId, String languageCode) {
        logger.info("Updating language preference for user {} to: {}", userId, languageCode);
        
        UserPreferences preferences = getUserPreferences(userId);
        preferences.setPreferredLanguage(languageCode);
        preferences.setUpdatedAt(LocalDateTime.now());
        
        return userPreferencesRepository.save(preferences);
    }
    
    /**
     * Update user's favorite tabs.
     * 
     * @param userId the user's UUID
     * @param tabs list of favorite tab names
     * @return the updated user preferences
     */
    public UserPreferences updateFavoriteTabs(UUID userId, List<String> tabs) {
        logger.info("Updating favorite tabs for user: {}", userId);
        
        UserPreferences preferences = getUserPreferences(userId);
        preferences.setFavoriteTabs(tabs);
        preferences.setUpdatedAt(LocalDateTime.now());
        
        return userPreferencesRepository.save(preferences);
    }
    
    /**
     * Update user's theme preference.
     * 
     * @param userId the user's UUID
     * @param theme the theme name (e.g., "light", "dark")
     * @return the updated user preferences
     */
    public UserPreferences updateTheme(UUID userId, String theme) {
        logger.info("Updating theme preference for user {} to: {}", userId, theme);
        
        UserPreferences preferences = getUserPreferences(userId);
        preferences.setTheme(theme);
        preferences.setUpdatedAt(LocalDateTime.now());
        
        return userPreferencesRepository.save(preferences);
    }
    
    /**
     * Create default preferences for a new user.
     * 
     * @param userId the user's UUID
     * @return the created user preferences
     */
    public UserPreferences createDefaultPreferences(UUID userId) {
        logger.info("Creating default preferences for user: {}", userId);
        
        UserPreferences preferences = new UserPreferences(userId);
        preferences.setPreferredLanguage("en");
        preferences.setTheme("light");
        preferences.setCreatedAt(LocalDateTime.now());
        preferences.setUpdatedAt(LocalDateTime.now());
        
        return userPreferencesRepository.save(preferences);
    }
    
    /**
     * Delete user preferences.
     * 
     * @param userId the user's UUID
     */
    public void deleteUserPreferences(UUID userId) {
        logger.info("Deleting preferences for user: {}", userId);
        
        userPreferencesRepository.findByUserId(userId)
                .ifPresent(userPreferencesRepository::delete);
    }
}
