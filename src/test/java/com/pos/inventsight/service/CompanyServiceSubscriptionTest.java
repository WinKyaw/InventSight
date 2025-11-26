package com.pos.inventsight.service;

import com.pos.inventsight.exception.PlanLimitExceededException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceSubscriptionTest {
    
    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private CompanyService companyService;
    
    private User testUser;
    private Company testCompany;
    
    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(UUID.randomUUID());
        testUser.setId(UUID.randomUUID());
        testUser.setSubscriptionLevel(SubscriptionLevel.FREE);
        
        testCompany = new Company("Test Company", "test@company.com");
        testCompany.setId(UUID.randomUUID());
    }
    
    @Test
    void createCompany_FreeUser_NoCompanies_Success() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(0L);
        when(companyRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(companyStoreUserRepository.save(any(CompanyStoreUser.class))).thenReturn(new CompanyStoreUser());
        
        // When
        Company result = companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        
        // Then
        assertNotNull(result);
        verify(companyStoreUserRepository, times(1)).countCompaniesByFounder(testUser);
        verify(companyRepository, times(1)).save(any(Company.class));
    }
    
    @Test
    void createCompany_FreeUser_OneCompany_ThrowsException() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(1L);
        
        // When & Then
        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class, () -> {
            companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        });
        
        assertTrue(exception.getMessage().contains("maximum number of companies"));
        assertTrue(exception.getMessage().contains("Free"));
        verify(companyStoreUserRepository, times(1)).countCompaniesByFounder(testUser);
        verify(companyRepository, never()).save(any(Company.class));
    }
    
    @Test
    void createCompany_ProUser_ThreeCompanies_Success() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.PRO);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(2L);
        when(companyRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(companyStoreUserRepository.save(any(CompanyStoreUser.class))).thenReturn(new CompanyStoreUser());
        
        // When
        Company result = companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        
        // Then
        assertNotNull(result);
        verify(companyRepository, times(1)).save(any(Company.class));
    }
    
    @Test
    void createCompany_ProUser_ReachedLimit_ThrowsException() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.PRO);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(3L);
        
        // When & Then
        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class, () -> {
            companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        });
        
        assertTrue(exception.getMessage().contains("maximum number of companies"));
        assertTrue(exception.getMessage().contains("3"));
        verify(companyRepository, never()).save(any(Company.class));
    }
    
    @Test
    void createCompany_BusinessUser_TenCompanies_Success() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.BUSINESS);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(9L);
        when(companyRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(companyStoreUserRepository.save(any(CompanyStoreUser.class))).thenReturn(new CompanyStoreUser());
        
        // When
        Company result = companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        
        // Then
        assertNotNull(result);
        verify(companyRepository, times(1)).save(any(Company.class));
    }
    
    @Test
    void createCompany_EnterpriseUser_Unlimited() {
        // Given
        testUser.setSubscriptionLevel(SubscriptionLevel.ENTERPRISE);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        // For ENTERPRISE users, countCompaniesByFounder is not called because it's unlimited
        when(companyRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(companyStoreUserRepository.save(any(CompanyStoreUser.class))).thenReturn(new CompanyStoreUser());
        
        // When
        Company result = companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        
        // Then
        assertNotNull(result);
        verify(companyRepository, times(1)).save(any(Company.class));
        // Verify that countCompaniesByFounder was not called for unlimited plans
        verify(companyStoreUserRepository, never()).countCompaniesByFounder(testUser);
    }
    
    @Test
    void createCompany_NullSubscriptionLevel_DefaultsToFree() {
        // Given
        testUser.setSubscriptionLevel(null);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.countCompaniesByFounder(testUser)).thenReturn(1L);
        
        // When & Then
        PlanLimitExceededException exception = assertThrows(PlanLimitExceededException.class, () -> {
            companyService.createCompany("New Company", "Description", "new@company.com", authentication);
        });
        
        assertTrue(exception.getMessage().contains("Free"));
        verify(companyRepository, never()).save(any(Company.class));
    }
}
