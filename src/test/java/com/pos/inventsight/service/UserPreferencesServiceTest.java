package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.UserPreferences;
import com.pos.inventsight.repository.sql.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserPreferencesServiceTest {
    
    @Mock
    private UserPreferencesRepository userPreferencesRepository;
    
    @InjectMocks
    private UserPreferencesService userPreferencesService;
    
    private UserPreferences testPreferences;
    private UUID testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPreferences = new UserPreferences(testUserId);
        testPreferences.setId(UUID.randomUUID());
        testPreferences.setPreferredLanguage("en");
        testPreferences.setTheme("light");
    }
    
    @Test
    void getUserPreferences_ExistingUser_Success() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        
        // When
        UserPreferences result = userPreferencesService.getUserPreferences(testUserId);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals("en", result.getPreferredLanguage());
        verify(userPreferencesRepository).findByUserId(testUserId);
    }
    
    @Test
    void getUserPreferences_NewUser_CreatesDefault() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        
        // When
        UserPreferences result = userPreferencesService.getUserPreferences(testUserId);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }
    
    @Test
    void updateLanguagePreference_Success() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        
        // When
        UserPreferences result = userPreferencesService.updateLanguagePreference(testUserId, "es");
        
        // Then
        assertNotNull(result);
        assertEquals("es", result.getPreferredLanguage());
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }
    
    @Test
    void updateFavoriteTabs_Success() {
        // Given
        List<String> tabs = Arrays.asList("dashboard", "inventory", "employees");
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        
        // When
        UserPreferences result = userPreferencesService.updateFavoriteTabs(testUserId, tabs);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.getFavoriteTabs().size());
        assertTrue(result.getFavoriteTabs().contains("dashboard"));
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }
    
    @Test
    void updateTheme_Success() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        
        // When
        UserPreferences result = userPreferencesService.updateTheme(testUserId, "dark");
        
        // Then
        assertNotNull(result);
        assertEquals("dark", result.getTheme());
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }
    
    @Test
    void createDefaultPreferences_Success() {
        // Given
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        
        // When
        UserPreferences result = userPreferencesService.createDefaultPreferences(testUserId);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals("en", result.getPreferredLanguage());
        assertEquals("light", result.getTheme());
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }
    
    @Test
    void deleteUserPreferences_Success() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        doNothing().when(userPreferencesRepository).delete(any(UserPreferences.class));
        
        // When
        userPreferencesService.deleteUserPreferences(testUserId);
        
        // Then
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository).delete(testPreferences);
    }
    
    @Test
    void deleteUserPreferences_NotFound_DoesNothing() {
        // Given
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        
        // When
        userPreferencesService.deleteUserPreferences(testUserId);
        
        // Then
        verify(userPreferencesRepository).findByUserId(testUserId);
        verify(userPreferencesRepository, never()).delete(any(UserPreferences.class));
    }
}
