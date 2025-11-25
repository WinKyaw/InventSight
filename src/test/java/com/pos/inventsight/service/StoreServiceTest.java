package com.pos.inventsight.service;

import com.pos.inventsight.dto.StoreRequest;
import com.pos.inventsight.dto.StoreResponse;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Store Service Unit Tests")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private UserStoreRoleRepository userStoreRoleRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ActivityLogService activityLogService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private StoreService storeService;
    
    private User testUser;
    private Store testStore;
    private StoreRequest storeRequest;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUuid(UUID.randomUUID());
        
        // Setup test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");
        testStore.setDescription("Test store description");
        testStore.setCreatedBy("testuser");
        testStore.setIsActive(true);
        
        // Setup store request
        storeRequest = new StoreRequest();
        storeRequest.setStoreName("New Store");
        storeRequest.setDescription("New store description");
        storeRequest.setAddress("123 Test Street");
        storeRequest.setCity("Test City");
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
    }
    
    @Test
    @DisplayName("Should create a new store successfully")
    void testCreateStore() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(storeRepository.existsByStoreName("New Store")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenReturn(testStore);
        when(userStoreRoleRepository.save(any(UserStoreRole.class))).thenReturn(new UserStoreRole());
        
        // Act
        StoreResponse result = storeService.createStore(storeRequest, authentication);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Store", result.getStoreName());
        verify(storeRepository).save(any(Store.class));
        verify(userStoreRoleRepository).save(any(UserStoreRole.class));
        verify(activityLogService).logActivity(anyString(), anyString(), eq("STORE_CREATED"), anyString(), anyString());
        
        System.out.println("✅ Test passed: Store created successfully");
    }
    
    @Test
    @DisplayName("Should get user stores")
    void testGetUserStores() {
        // Arrange
        UserStoreRole userStoreRole = new UserStoreRole(testUser, testStore, UserRole.OWNER, "testuser");
        List<UserStoreRole> userStoreRoles = Arrays.asList(userStoreRole);
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userStoreRoleRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(userStoreRoles);
        
        // Act
        List<StoreResponse> result = storeService.getUserStores(authentication);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Store", result.get(0).getStoreName());
        
        System.out.println("✅ Test passed: Retrieved user stores successfully");
    }
    
    @Test
    @DisplayName("Should activate store and set tenant context")
    void testActivateStore() {
        // Arrange
        UUID storeId = testStore.getId();
        UserStoreRole userStoreRole = new UserStoreRole(testUser, testStore, UserRole.OWNER, "testuser");
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(testUser, testStore))
                .thenReturn(Optional.of(userStoreRole));
        
        // Act
        StoreResponse result = storeService.activateStore(storeId, authentication);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Store", result.getStoreName());
        verify(activityLogService).logActivity(anyString(), anyString(), eq("STORE_ACTIVATED"), anyString(), anyString());
        
        System.out.println("✅ Test passed: Store activated successfully");
        System.out.println("   Store: " + result.getStoreName());
        System.out.println("   Store ID: " + result.getId());
    }
    
    @Test
    @DisplayName("Should throw exception when store not found for activation")
    void testActivateStoreNotFound() {
        // Arrange
        UUID storeId = UUID.randomUUID();
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            storeService.activateStore(storeId, authentication);
        });
        
        System.out.println("✅ Test passed: Exception thrown for non-existent store");
    }
    
    @Test
    @DisplayName("Should throw exception when authentication is null")
    void testCreateStoreWithoutAuthentication() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            storeService.createStore(storeRequest, null);
        });
        
        System.out.println("✅ Test passed: Exception thrown for null authentication");
    }
}