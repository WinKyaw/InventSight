package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("User Service Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserStoreRoleRepository userStoreRoleRepository;
    
    @Mock
    private ActivityLogService activityLogService;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private Store testStore;
    private UUID testUuid;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test UUID
        testUuid = UUID.fromString("87b6a00e-896a-4f69-b9cd-3349d50c1578");
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUuid(testUuid);
        
        // Setup test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");
        testStore.setDescription("Test store description");
        testStore.setCreatedBy("testuser");
        testStore.setIsActive(true);
    }
    
    @AfterEach
    void tearDown() {
        // Always clear tenant context after each test
        TenantContext.clear();
    }
    
    @Test
    @DisplayName("Test extractUuidFromTenantId - Company Schema Format")
    void testExtractUuidFromTenantId_CompanySchema() throws Exception {
        // Given a company schema format
        String companySchema = "company_87b6a00e_896a_4f69_b9cd_3349d50c1578";
        TenantContext.setCurrentTenant(companySchema);
        
        // Setup mocks
        UserStoreRole userStoreRole = new UserStoreRole(testUser, testStore, UserRole.OWNER, "testuser");
        when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(List.of(userStoreRole));
        
        // When getting current user store
        Store result = userService.getCurrentUserStore();
        
        // Then should extract UUID correctly and find the store
        assertNotNull(result);
        assertEquals(testStore.getId(), result.getId());
        
        // Verify the UUID was extracted correctly (testUuid should match what was looked up)
        verify(userRepository).findByUuid(testUuid);
    }
    
    @Test
    @DisplayName("Test extractUuidFromTenantId - Raw UUID Format")
    void testExtractUuidFromTenantId_RawUuid() throws Exception {
        // Given a raw UUID format
        String rawUuid = "87b6a00e-896a-4f69-b9cd-3349d50c1578";
        TenantContext.setCurrentTenant(rawUuid);
        
        // Setup mocks
        UserStoreRole userStoreRole = new UserStoreRole(testUser, testStore, UserRole.OWNER, "testuser");
        when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(List.of(userStoreRole));
        
        // When getting current user store
        Store result = userService.getCurrentUserStore();
        
        // Then should use UUID directly and find the store
        assertNotNull(result);
        assertEquals(testStore.getId(), result.getId());
        
        // Verify the UUID was used directly
        verify(userRepository).findByUuid(testUuid);
    }
    
    @Test
    @DisplayName("Test getCurrentUserStore - With Company Schema")
    void testGetCurrentUserStore_WithCompanySchema() {
        // Given a company schema in tenant context
        String companySchema = "company_87b6a00e_896a_4f69_b9cd_3349d50c1578";
        TenantContext.setCurrentTenant(companySchema);
        
        // Setup mocks
        UserStoreRole userStoreRole = new UserStoreRole(testUser, testStore, UserRole.OWNER, "testuser");
        when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(List.of(userStoreRole));
        
        // When getting current user store
        Store result = userService.getCurrentUserStore();
        
        // Then should find the store successfully
        assertNotNull(result);
        assertEquals(testStore.getId(), result.getId());
        assertEquals("Test Store", result.getStoreName());
    }
    
    @Test
    @DisplayName("Test getCurrentUserStore - With Default Tenant")
    void testGetCurrentUserStore_WithDefaultTenant() {
        // Given default tenant
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        
        // When getting current user store
        Store result = userService.getCurrentUserStore();
        
        // Then should return null
        assertNull(result);
        
        // Verify no repository calls were made
        verify(userRepository, never()).findByUuid(any());
    }
    
    @Test
    @DisplayName("Test getCurrentUserStore - Invalid UUID Format")
    void testGetCurrentUserStore_InvalidUuidFormat() {
        // Given an invalid UUID format
        TenantContext.setCurrentTenant("invalid-uuid-format");
        
        // When/Then should throw ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.getCurrentUserStore()
        );
        
        assertTrue(exception.getMessage().contains("Invalid UUID format for tenant"));
    }
    
    @Test
    @DisplayName("Test getCurrentUserStore - User Not Found")
    void testGetCurrentUserStore_UserNotFound() {
        // Given a valid UUID but user doesn't exist
        String tenantId = "87b6a00e-896a-4f69-b9cd-3349d50c1578";
        TenantContext.setCurrentTenant(tenantId);
        
        // Setup mocks
        when(userRepository.findByUuid(testUuid)).thenReturn(Optional.empty());
        
        // When/Then should throw ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.getCurrentUserStore()
        );
        
        assertTrue(exception.getMessage().contains("User not found for tenant"));
    }
    
    @Test
    @DisplayName("Test getCurrentUserStore - No Active Store")
    void testGetCurrentUserStore_NoActiveStore() {
        // Given a user with no active stores
        String companySchema = "company_87b6a00e_896a_4f69_b9cd_3349d50c1578";
        TenantContext.setCurrentTenant(companySchema);
        
        // Setup mocks
        when(userRepository.findByUuid(testUuid)).thenReturn(Optional.of(testUser));
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.emptyList());
        
        // When/Then should throw ResourceNotFoundException
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.getCurrentUserStore()
        );
        
        assertTrue(exception.getMessage().contains("No active store found for user"));
    }
    
    @Test
    @DisplayName("Test extractUuidFromTenantId - Multiple Underscores in UUID")
    void testExtractUuidFromTenantId_MultipleUnderscores() {
        // Given a company schema with UUID containing multiple underscore segments
        String companySchema = "company_12345678_1234_1234_1234_123456789012";
        TenantContext.setCurrentTenant(companySchema);
        
        // Setup expected UUID
        UUID expectedUuid = UUID.fromString("12345678-1234-1234-1234-123456789012");
        User user = new User();
        user.setId(1L);
        user.setUuid(expectedUuid);
        
        // Setup mocks
        UserStoreRole userStoreRole = new UserStoreRole(user, testStore, UserRole.OWNER, "testuser");
        when(userRepository.findByUuid(expectedUuid)).thenReturn(Optional.of(user));
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(user))
            .thenReturn(List.of(userStoreRole));
        
        // When getting current user store
        Store result = userService.getCurrentUserStore();
        
        // Then should extract UUID correctly
        assertNotNull(result);
        verify(userRepository).findByUuid(expectedUuid);
    }
}
