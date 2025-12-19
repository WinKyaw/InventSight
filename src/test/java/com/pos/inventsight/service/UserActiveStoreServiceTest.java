package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserActiveStore;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.repository.sql.UserActiveStoreRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("User Active Store Service Unit Tests")
class UserActiveStoreServiceTest {

    @Mock
    private UserActiveStoreRepository userActiveStoreRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @InjectMocks
    private UserActiveStoreService userActiveStoreService;
    
    private User testUser;
    private Store testStore;
    private Company testCompany;
    private UserActiveStore testActiveStore;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");
        
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.EMPLOYEE);
        
        // Setup test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");
        testStore.setDescription("Test store description");
        testStore.setCompany(testCompany);
        testStore.setIsActive(true);
        
        // Setup test active store
        testActiveStore = new UserActiveStore();
        testActiveStore.setId(UUID.randomUUID());
        testActiveStore.setUser(testUser);
        testActiveStore.setStore(testStore);
    }
    
    @Test
    @DisplayName("Should get user's active store")
    void testGetUserActiveStore() {
        // Given
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.of(testActiveStore));
        
        // When
        Optional<Store> result = userActiveStoreService.getUserActiveStore(testUser.getId());
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testStore.getId(), result.get().getId());
        assertEquals(testStore.getStoreName(), result.get().getStoreName());
        verify(userActiveStoreRepository).findByUserId(testUser.getId());
    }
    
    @Test
    @DisplayName("Should return empty when user has no active store")
    void testGetUserActiveStoreWhenNotSet() {
        // Given
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.empty());
        
        // When
        Optional<Store> result = userActiveStoreService.getUserActiveStore(testUser.getId());
        
        // Then
        assertFalse(result.isPresent());
        verify(userActiveStoreRepository).findByUserId(testUser.getId());
    }
    
    @Test
    @DisplayName("Should set active store for user with store access")
    void testSetUserActiveStore() {
        // Given
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setCompany(testCompany);
        csu.setStore(testStore);
        csu.setUser(testUser);
        csu.setRole(CompanyRole.EMPLOYEE);
        
        when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(csu));
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.empty());
        when(userActiveStoreRepository.save(any(UserActiveStore.class)))
            .thenReturn(testActiveStore);
        
        // When
        userActiveStoreService.setUserActiveStore(testUser, testStore.getId());
        
        // Then
        verify(storeRepository).findById(testStore.getId());
        verify(companyStoreUserRepository).findByUserAndIsActiveTrue(testUser);
        verify(userActiveStoreRepository).findByUserId(testUser.getId());
        verify(userActiveStoreRepository).save(any(UserActiveStore.class));
    }
    
    @Test
    @DisplayName("Should update existing active store")
    void testUpdateExistingActiveStore() {
        // Given
        Store newStore = new Store();
        newStore.setId(UUID.randomUUID());
        newStore.setStoreName("New Store");
        newStore.setCompany(testCompany);
        
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setCompany(testCompany);
        csu.setStore(newStore);
        csu.setUser(testUser);
        csu.setRole(CompanyRole.EMPLOYEE);
        
        when(storeRepository.findById(newStore.getId()))
            .thenReturn(Optional.of(newStore));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(csu));
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.of(testActiveStore));
        when(userActiveStoreRepository.save(any(UserActiveStore.class)))
            .thenReturn(testActiveStore);
        
        // When
        userActiveStoreService.setUserActiveStore(testUser, newStore.getId());
        
        // Then
        verify(storeRepository).findById(newStore.getId());
        verify(userActiveStoreRepository).findByUserId(testUser.getId());
        verify(userActiveStoreRepository).save(testActiveStore);
    }
    
    @Test
    @DisplayName("Should throw exception when store not found")
    void testSetUserActiveStoreStoreNotFound() {
        // Given
        UUID nonExistentStoreId = UUID.randomUUID();
        when(storeRepository.findById(nonExistentStoreId))
            .thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            userActiveStoreService.setUserActiveStore(testUser, nonExistentStoreId);
        });
        
        verify(storeRepository).findById(nonExistentStoreId);
        verify(userActiveStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when user has no access to store")
    void testSetUserActiveStoreNoAccess() {
        // Given
        when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList()); // No store access
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            userActiveStoreService.setUserActiveStore(testUser, testStore.getId());
        });
        
        verify(storeRepository).findById(testStore.getId());
        verify(companyStoreUserRepository).findByUserAndIsActiveTrue(testUser);
        verify(userActiveStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should allow GM to access any store in company")
    void testSetUserActiveStoreGMAccess() {
        // Given
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setCompany(testCompany);
        csu.setStore(null); // Company-level role
        csu.setUser(testUser);
        csu.setRole(CompanyRole.GENERAL_MANAGER);
        
        when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(csu));
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.empty());
        when(userActiveStoreRepository.save(any(UserActiveStore.class)))
            .thenReturn(testActiveStore);
        
        // When
        userActiveStoreService.setUserActiveStore(testUser, testStore.getId());
        
        // Then
        verify(userActiveStoreRepository).save(any(UserActiveStore.class));
    }
    
    @Test
    @DisplayName("Should throw exception when getting active store for user without one")
    void testGetUserActiveStoreOrThrowWhenNotSet() {
        // Given
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            userActiveStoreService.getUserActiveStoreOrThrow(testUser.getId());
        });
    }
    
    @Test
    @DisplayName("Should initialize active store for new user")
    void testInitializeUserActiveStore() {
        // Given
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setCompany(testCompany);
        csu.setStore(testStore);
        csu.setUser(testUser);
        csu.setRole(CompanyRole.EMPLOYEE);
        
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.empty());
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(csu));
        when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));
        when(userActiveStoreRepository.save(any(UserActiveStore.class)))
            .thenReturn(testActiveStore);
        
        // When
        userActiveStoreService.initializeUserActiveStore(testUser);
        
        // Then
        verify(userActiveStoreRepository, times(2)).findByUserId(testUser.getId()); // Called in initialize and set
        verify(companyStoreUserRepository, times(2)).findByUserAndIsActiveTrue(testUser); // Called in getFirstAvailableStore and canUserAccessStore
        verify(userActiveStoreRepository).save(any(UserActiveStore.class));
    }
    
    @Test
    @DisplayName("Should not reinitialize active store if already set")
    void testInitializeUserActiveStoreAlreadySet() {
        // Given
        when(userActiveStoreRepository.findByUserId(testUser.getId()))
            .thenReturn(Optional.of(testActiveStore));
        
        // When
        userActiveStoreService.initializeUserActiveStore(testUser);
        
        // Then
        verify(userActiveStoreRepository).findByUserId(testUser.getId());
        verify(userActiveStoreRepository, never()).save(any());
    }
}
