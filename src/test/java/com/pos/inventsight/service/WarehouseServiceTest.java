package com.pos.inventsight.service;

import com.pos.inventsight.dto.WarehouseRequest;
import com.pos.inventsight.dto.WarehouseResponse;
import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for WarehouseService to verify company_id assignment during warehouse creation
 */
@DisplayName("Warehouse Service Unit Tests - Company Assignment")
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ActivityLogService activityLogService;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private WarehouseService warehouseService;
    
    private User testUser;
    private Company testCompany;
    private Warehouse testWarehouse;
    private WarehouseRequest warehouseRequest;
    private CompanyStoreUser testMembership;
    
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
        
        // Setup test company
        testCompany = new Company();
        testCompany.setId(UUID.fromString("9c00c23a-ee97-4bc6-86bb-5a85a7f5dca5"));
        testCompany.setName("Test Company");
        testCompany.setIsActive(true);
        
        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setId(UUID.randomUUID());
        testWarehouse.setName("Test Warehouse");
        testWarehouse.setLocation("Test Location");
        testWarehouse.setCompany(testCompany);
        testWarehouse.setIsActive(true);
        
        // Setup warehouse request
        warehouseRequest = new WarehouseRequest();
        warehouseRequest.setName("New Warehouse");
        warehouseRequest.setLocation("New Location");
        warehouseRequest.setAddress("123 Test Street");
        warehouseRequest.setCity("Test City");
        warehouseRequest.setIsActive(true);
        
        // Setup company membership
        testMembership = new CompanyStoreUser();
        testMembership.setId(UUID.randomUUID());
        testMembership.setUser(testUser);
        testMembership.setCompany(testCompany);
        testMembership.setIsActive(true);
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Clear tenant context before each test
        TenantContext.clear();
    }
    
    @AfterEach
    void tearDown() {
        // Clear tenant context after each test
        TenantContext.clear();
    }
    
    @Test
    @DisplayName("Should create warehouse with company from tenant context")
    void testCreateWarehouse_WithTenantContext() {
        // Arrange
        String tenantId = "company_9c00c23a_ee97_4bc6_86bb_5a85a7f5dca5";
        TenantContext.setCurrentTenant(tenantId);
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.findById(testCompany.getId())).thenReturn(Optional.of(testCompany));
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse warehouse = invocation.getArgument(0);
            warehouse.setId(UUID.randomUUID());
            return warehouse;
        });
        
        // Act
        WarehouseResponse result = warehouseService.createWarehouse(warehouseRequest, authentication);
        
        // Assert
        assertNotNull(result);
        assertEquals("New Warehouse", result.getName());
        
        // Verify that save was called with a warehouse that has company set
        ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(warehouseCaptor.capture());
        
        Warehouse savedWarehouse = warehouseCaptor.getValue();
        assertNotNull(savedWarehouse.getCompany(), "Company should be set on warehouse");
        assertEquals(testCompany.getId(), savedWarehouse.getCompany().getId(), "Company ID should match");
        
        verify(activityLogService).logActivity(
            eq(testUser.getId().toString()),
            eq("testuser"),
            eq("warehouse_created"),
            eq("warehouse"),
            contains("Test Company")
        );
    }
    
    @Test
    @DisplayName("Should create warehouse with company from user membership when no tenant context")
    void testCreateWarehouse_WithUserMembership() {
        // Arrange - no tenant context set
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(testMembership));
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse warehouse = invocation.getArgument(0);
            warehouse.setId(UUID.randomUUID());
            return warehouse;
        });
        
        // Act
        WarehouseResponse result = warehouseService.createWarehouse(warehouseRequest, authentication);
        
        // Assert
        assertNotNull(result);
        
        // Verify that save was called with a warehouse that has company set
        ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(warehouseCaptor.capture());
        
        Warehouse savedWarehouse = warehouseCaptor.getValue();
        assertNotNull(savedWarehouse.getCompany(), "Company should be set on warehouse");
        assertEquals(testCompany.getId(), savedWarehouse.getCompany().getId(), "Company ID should match");
    }
    
    @Test
    @DisplayName("Should fall back to user membership when tenant context extraction fails")
    void testCreateWarehouse_FallbackToUserMembership() {
        // Arrange - invalid tenant context
        TenantContext.setCurrentTenant("company_invalid_uuid");
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(testMembership));
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse warehouse = invocation.getArgument(0);
            warehouse.setId(UUID.randomUUID());
            return warehouse;
        });
        
        // Act
        WarehouseResponse result = warehouseService.createWarehouse(warehouseRequest, authentication);
        
        // Assert
        assertNotNull(result);
        
        // Verify fallback to user membership was used
        verify(companyStoreUserRepository).findByUserAndIsActiveTrue(testUser);
        
        ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(warehouseCaptor.capture());
        
        Warehouse savedWarehouse = warehouseCaptor.getValue();
        assertNotNull(savedWarehouse.getCompany(), "Company should be set on warehouse");
    }
    
    @Test
    @DisplayName("Should throw exception when user has no active company membership")
    void testCreateWarehouse_NoCompanyMembership() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.emptyList());
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(false);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            warehouseService.createWarehouse(warehouseRequest, authentication);
        });
        
        assertTrue(exception.getMessage().contains("no active company membership"));
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should throw exception when company not found in tenant context")
    void testCreateWarehouse_CompanyNotFound() {
        // Arrange
        String tenantId = "company_9c00c23a_ee97_4bc6_86bb_5a85a7f5dca5";
        TenantContext.setCurrentTenant(tenantId);
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyRepository.findById(testCompany.getId())).thenReturn(Optional.empty());
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.emptyList());
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(false);
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            warehouseService.createWarehouse(warehouseRequest, authentication);
        });
        
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should throw exception when warehouse name already exists")
    void testCreateWarehouse_DuplicateName() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Arrays.asList(testMembership));
        when(warehouseRepository.existsByNameIgnoreCaseAndIsActiveTrue("New Warehouse")).thenReturn(true);
        
        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            warehouseService.createWarehouse(warehouseRequest, authentication);
        });
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should throw exception when authentication is null")
    void testCreateWarehouse_NullAuthentication() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            warehouseService.createWarehouse(warehouseRequest, null);
        });
        
        assertTrue(exception.getMessage().contains("Authentication is required"));
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
    
    @Test
    @DisplayName("Should get warehouse by ID")
    void testGetWarehouseById() {
        // Arrange
        UUID warehouseId = testWarehouse.getId();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        
        // Act
        WarehouseResponse result = warehouseService.getWarehouseById(warehouseId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testWarehouse.getName(), result.getName());
        verify(warehouseRepository).findById(warehouseId);
    }
    
    @Test
    @DisplayName("Should throw exception when warehouse not found")
    void testGetWarehouseById_NotFound() {
        // Arrange
        UUID warehouseId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            warehouseService.getWarehouseById(warehouseId);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
    }
}
