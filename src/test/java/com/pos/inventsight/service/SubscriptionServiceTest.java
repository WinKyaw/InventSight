package com.pos.inventsight.service;

import com.pos.inventsight.dto.SubscriptionInfoResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.SubscriptionLevel;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private SubscriptionService subscriptionService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(UUID.randomUUID());
        testUser.setSubscriptionLevel(SubscriptionLevel.FREE);
    }
    
    @Test
    void getSubscriptionInfo_FreeUser_NoCompanies() {
        // Given
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(0L);
        
        // When
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(testUser);
        
        // Then
        assertNotNull(response);
        assertEquals("FREE", response.getPlan());
        assertEquals(1, response.getMaxCompanies());
        assertEquals(0L, response.getCurrentUsage());
        assertEquals(1, response.getRemaining());
        
        verify(companyStoreUserRepository, times(1)).countCompaniesByFounder(testUser);
    }
    
    @Test
    void getSubscriptionInfo_FreeUser_OneCompany() {
        // Given
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(1L);
        
        // When
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(testUser);
        
        // Then
        assertNotNull(response);
        assertEquals("FREE", response.getPlan());
        assertEquals(1, response.getMaxCompanies());
        assertEquals(1L, response.getCurrentUsage());
        assertEquals(0, response.getRemaining());
    }
    
    @Test
    void getSubscriptionInfo_ProUser_TwoCompanies() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.PRO);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(2L);
        
        // When
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(testUser);
        
        // Then
        assertNotNull(response);
        assertEquals("PRO", response.getPlan());
        assertEquals(3, response.getMaxCompanies());
        assertEquals(2L, response.getCurrentUsage());
        assertEquals(1, response.getRemaining());
    }
    
    @Test
    void getSubscriptionInfo_EnterpriseUser_Unlimited() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.ENTERPRISE);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(100L);
        
        // When
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(testUser);
        
        // Then
        assertNotNull(response);
        assertEquals("ENTERPRISE", response.getPlan());
        assertNull(response.getMaxCompanies());
        assertEquals(100L, response.getCurrentUsage());
        assertNull(response.getRemaining());
    }
    
    @Test
    void getSubscriptionInfo_NullSubscriptionLevel_DefaultsToFree() {
        // Given
        testUser.setSubscriptionLevel(null);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(0L);
        
        // When
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(testUser);
        
        // Then
        assertNotNull(response);
        assertEquals("FREE", response.getPlan());
        assertEquals(1, response.getMaxCompanies());
    }
    
    @Test
    void updateSubscription_Success() {
        // Given
        UUID userId = testUser.getId();
        when(userService.getUserById(userId)).thenReturn(testUser);
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        
        // When
        User result = subscriptionService.updateSubscription(userId, "PRO");
        
        // Then
        assertNotNull(result);
        assertEquals(SubscriptionLevel.PRO, result.getSubscriptionLevel());
        
        verify(userService, times(1)).getUserById(userId);
        verify(userService, times(1)).saveUser(testUser);
    }
    
    @Test
    void updateSubscription_InvalidLevel_ThrowsException() {
        // Given
        UUID userId = testUser.getId();
        when(userService.getUserById(userId)).thenReturn(testUser);
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            subscriptionService.updateSubscription(userId, "INVALID");
        });
        
        verify(userService, times(1)).getUserById(userId);
        verify(userService, never()).saveUser(any(User.class));
    }
    
    @Test
    void updateSubscription_CaseInsensitive() {
        // Given
        UUID userId = testUser.getId();
        when(userService.getUserById(userId)).thenReturn(testUser);
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        
        // When
        User result = subscriptionService.updateSubscription(userId, "business");
        
        // Then
        assertNotNull(result);
        assertEquals(SubscriptionLevel.BUSINESS, result.getSubscriptionLevel());
    }
}
