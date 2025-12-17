package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.UserNavigationPreference;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.UserNavigationPreferenceRepository;
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
public class UserNavigationPreferenceServiceTest {
    
    @Mock
    private UserNavigationPreferenceRepository navigationPreferenceRepository;
    
    @InjectMocks
    private UserNavigationPreferenceService navigationPreferenceService;
    
    private UserNavigationPreference testPreferences;
    private UUID testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPreferences = new UserNavigationPreference(testUserId);
        testPreferences.setId(UUID.randomUUID());
        testPreferences.setAvailableTabs(Arrays.asList("items", "receipt", "employees"));
        testPreferences.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
    }
    
    @Test
    void getNavigationPreferences_ExistingUser_Success() {
        // Given
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        
        // When
        UserNavigationPreference result = navigationPreferenceService.getNavigationPreferences(testUserId, UserRole.MANAGER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(3, result.getAvailableTabs().size());
        verify(navigationPreferenceRepository).findByUserId(testUserId);
    }
    
    @Test
    void getNavigationPreferences_NewUser_CreatesDefault() {
        // Given
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(testPreferences);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.getNavigationPreferences(testUserId, UserRole.OWNER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_ManagerRole_ReturnsGMPlusTabs() {
        // Given
        UserNavigationPreference gmPrefs = new UserNavigationPreference(testUserId);
        gmPrefs.setId(UUID.randomUUID());
        gmPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "employees", "calendar", "dashboard", "reports", "warehouse", "settings"));
        gmPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(gmPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.MANAGER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("employees"));
        // Verify GM+ roles have access to additional tabs
        assertTrue(result.getAvailableTabs().contains("calendar"));
        assertTrue(result.getAvailableTabs().contains("dashboard"));
        assertTrue(result.getAvailableTabs().contains("reports"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_OwnerRole_ReturnsGMPlusTabs() {
        // Given
        UserNavigationPreference gmPrefs = new UserNavigationPreference(testUserId);
        gmPrefs.setId(UUID.randomUUID());
        gmPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "employees", "calendar", "dashboard", "reports", "warehouse", "settings"));
        gmPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(gmPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.OWNER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("employees"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_CoOwnerRole_ReturnsGMPlusTabs() {
        // Given
        UserNavigationPreference gmPrefs = new UserNavigationPreference(testUserId);
        gmPrefs.setId(UUID.randomUUID());
        gmPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "employees", "calendar", "dashboard", "reports", "warehouse", "settings"));
        gmPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(gmPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.CO_OWNER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("employees"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_FounderRole_ReturnsGMPlusTabs() {
        // Given
        UserNavigationPreference gmPrefs = new UserNavigationPreference(testUserId);
        gmPrefs.setId(UUID.randomUUID());
        gmPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "employees", "calendar", "dashboard", "reports", "warehouse", "settings"));
        gmPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(gmPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.FOUNDER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("employees"));
        // Verify FOUNDER (GM+ role) has access to team management
        assertTrue(result.getAvailableTabs().contains("calendar"));
        assertTrue(result.getAvailableTabs().contains("dashboard"));
        assertTrue(result.getAvailableTabs().contains("reports"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_AdminRole_ReturnsGMPlusTabs() {
        // Given
        UserNavigationPreference gmPrefs = new UserNavigationPreference(testUserId);
        gmPrefs.setId(UUID.randomUUID());
        gmPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "employees", "calendar", "dashboard", "reports", "warehouse", "settings"));
        gmPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "employees"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(gmPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.ADMIN);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("employees"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_EmployeeRole_ReturnsEmployeeTabs() {
        // Given
        UserNavigationPreference employeePrefs = new UserNavigationPreference(testUserId);
        employeePrefs.setId(UUID.randomUUID());
        employeePrefs.setAvailableTabs(Arrays.asList("items", "receipt", "calendar", "dashboard", "reports", "warehouse", "settings"));
        employeePrefs.setPreferredTabs(Arrays.asList("items", "receipt", "calendar"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(employeePrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.EMPLOYEE);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("calendar"));
        assertTrue(result.getAvailableTabs().contains("dashboard"));
        // Verify employees do NOT have access to "employees" tab (team management)
        assertFalse(result.getAvailableTabs().contains("employees"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_CashierRole_ReturnsEmployeeTabs() {
        // Given
        UserNavigationPreference cashierPrefs = new UserNavigationPreference(testUserId);
        cashierPrefs.setId(UUID.randomUUID());
        cashierPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "calendar", "dashboard", "reports", "warehouse", "settings"));
        cashierPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "calendar"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(cashierPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, UserRole.CASHIER);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("items"));
        assertTrue(result.getAvailableTabs().contains("receipt"));
        assertTrue(result.getAvailableTabs().contains("calendar"));
        assertFalse(result.getAvailableTabs().contains("employees"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void createDefaultPreferences_NullRole_ReturnsEmployeeTabs() {
        // Given
        UserNavigationPreference defaultPrefs = new UserNavigationPreference(testUserId);
        defaultPrefs.setId(UUID.randomUUID());
        defaultPrefs.setAvailableTabs(Arrays.asList("items", "receipt", "calendar", "dashboard", "reports", "warehouse", "settings"));
        defaultPrefs.setPreferredTabs(Arrays.asList("items", "receipt", "calendar"));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(defaultPrefs);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.createDefaultPreferences(testUserId, null);
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getAvailableTabs().contains("calendar"));
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void updateNavigationPreferences_ValidTabs_Success() {
        // Given
        List<String> newPreferredTabs = Arrays.asList("items", "receipt");
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        when(navigationPreferenceRepository.save(any(UserNavigationPreference.class))).thenReturn(testPreferences);
        
        // When
        UserNavigationPreference result = navigationPreferenceService.updateNavigationPreferences(testUserId, newPreferredTabs);
        
        // Then
        assertNotNull(result);
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void updateNavigationPreferences_InvalidTab_ThrowsException() {
        // Given
        List<String> invalidTabs = Arrays.asList("items", "unauthorized_tab");
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            navigationPreferenceService.updateNavigationPreferences(testUserId, invalidTabs);
        });
        
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository, never()).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void updateNavigationPreferences_NotFound_ThrowsException() {
        // Given
        List<String> tabs = Arrays.asList("items", "receipt");
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        
        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> {
            navigationPreferenceService.updateNavigationPreferences(testUserId, tabs);
        });
        
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository, never()).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void updateNavigationPreferences_EmployeeTriesToAccessTeam_ThrowsException() {
        // Given - Employee preferences without "employees" in available tabs
        UserNavigationPreference employeePrefs = new UserNavigationPreference(testUserId);
        employeePrefs.setId(UUID.randomUUID());
        employeePrefs.setAvailableTabs(Arrays.asList("items", "receipt", "calendar", "dashboard", "reports", "warehouse", "settings"));
        employeePrefs.setPreferredTabs(Arrays.asList("items", "receipt", "calendar"));
        
        List<String> invalidTabsWithTeam = Arrays.asList("items", "receipt", "employees");
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(employeePrefs));
        
        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            navigationPreferenceService.updateNavigationPreferences(testUserId, invalidTabsWithTeam);
        });
        
        // Verify the exception message mentions team access denied
        assertTrue(exception.getMessage().contains("Access denied") || 
                   exception.getMessage().contains("not available"));
        
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository, never()).save(any(UserNavigationPreference.class));
    }
    
    @Test
    void deleteNavigationPreferences_ExistingPreferences_Success() {
        // Given
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.of(testPreferences));
        doNothing().when(navigationPreferenceRepository).delete(any(UserNavigationPreference.class));
        
        // When
        navigationPreferenceService.deleteNavigationPreferences(testUserId);
        
        // Then
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository).delete(testPreferences);
    }
    
    @Test
    void deleteNavigationPreferences_NotFound_DoesNothing() {
        // Given
        when(navigationPreferenceRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        
        // When
        navigationPreferenceService.deleteNavigationPreferences(testUserId);
        
        // Then
        verify(navigationPreferenceRepository).findByUserId(testUserId);
        verify(navigationPreferenceRepository, never()).delete(any(UserNavigationPreference.class));
    }
}
