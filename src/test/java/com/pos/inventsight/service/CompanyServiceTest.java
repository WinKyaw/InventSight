package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {
    
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
    private CompanyStoreUser testCompanyStoreUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(UUID.randomUUID());
        testUser.setUuid(UUID.randomUUID());
        
        testCompany = new Company("Test Company", "test@company.com");
        testCompany.setId(UUID.randomUUID());
        
        testCompanyStoreUser = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER);
    }
    
    @Test
    void createCompany_Success() {
        // Given
        String companyName = "New Company";
        String description = "Test Description";
        String email = "new@company.com";
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.existsByNameIgnoreCase(companyName)).thenReturn(false);
        when(companyRepository.existsByEmail(email)).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(companyStoreUserRepository.save(any(CompanyStoreUser.class))).thenReturn(testCompanyStoreUser);
        
        // When
        Company result = companyService.createCompany(companyName, description, email, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(testCompany.getId(), result.getId());
        verify(companyRepository).save(any(Company.class));
        verify(companyStoreUserRepository).save(any(CompanyStoreUser.class));
    }
    
    @Test
    void createCompany_DuplicateName_ThrowsException() {
        // Given
        String companyName = "Existing Company";
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.existsByNameIgnoreCase(companyName)).thenReturn(true);
        
        // When & Then
        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> companyService.createCompany(companyName, "desc", "email@test.com", authentication)
        );
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(companyRepository, never()).save(any(Company.class));
    }
    
    @Test
    void createCompany_DuplicateEmail_ThrowsException() {
        // Given
        String email = "existing@company.com";
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(companyRepository.existsByEmail(email)).thenReturn(true);
        
        // When & Then
        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> companyService.createCompany("New Company", "desc", email, authentication)
        );
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(companyRepository, never()).save(any(Company.class));
    }
    
    @Test
    void getCompany_Success() {
        // Given
        UUID companyId = testCompany.getId();
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(testUser, testCompany)).thenReturn(true);
        
        // When
        Company result = companyService.getCompany(companyId, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(testCompany.getId(), result.getId());
    }
    
    @Test
    void getCompany_NotFound_ThrowsException() {
        // Given
        UUID companyId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> companyService.getCompany(companyId, authentication)
        );
        
        assertTrue(exception.getMessage().contains("not found"));
    }
    
    @Test
    void getCompany_NoAccess_ThrowsException() {
        // Given
        UUID companyId = testCompany.getId();
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(testUser, testCompany)).thenReturn(false);
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> companyService.getCompany(companyId, authentication)
        );
        
        assertTrue(exception.getMessage().contains("don't have access"));
    }
    
    @Test
    void hasCompanyAccess_True() {
        // Given
        when(companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(testUser, testCompany)).thenReturn(true);
        
        // When
        boolean result = companyService.hasCompanyAccess(testUser, testCompany);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void hasCompanyAccess_False() {
        // Given
        when(companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(testUser, testCompany)).thenReturn(false);
        
        // When
        boolean result = companyService.hasCompanyAccess(testUser, testCompany);
        
        // Then
        assertFalse(result);
    }
}