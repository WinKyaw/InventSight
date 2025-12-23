package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for warehouse permissions endpoint
 */
public class WarehousePermissionsEndpointTest {

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private UserService userService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WarehouseInventoryController controller;

    private UUID warehouseId;
    private UUID userId;
    private UUID companyId;
    private User testUser;
    private Company testCompany;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        warehouseId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        // Setup test company
        testCompany = new Company();
        testCompany.setId(companyId);
        testCompany.setName("Test Company");

        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.EMPLOYEE);

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setId(warehouseId);
        testWarehouse.setName("Test Warehouse");
        testWarehouse.setLocation("Test Location");
        testWarehouse.setCompany(testCompany);

        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    void testCheckWarehousePermissions_OWNER_HasFullPermissions() {
        // Given: User with OWNER role
        testUser.setRole(UserRole.OWNER);
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(new ArrayList<>());
        when(warehouseInventoryService.getUserCompanyRole(testUser)).thenReturn(CompanyRole.FOUNDER);

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> permissions = (Map<String, Object>) body.get("permissions");
        assertTrue((Boolean) permissions.get("canRead"));
        assertTrue((Boolean) permissions.get("canWrite"));
        assertTrue((Boolean) permissions.get("canAddInventory"));
        assertTrue((Boolean) permissions.get("canWithdrawInventory"));
    }

    @Test
    void testCheckWarehousePermissions_GeneralManager_HasFullPermissions() {
        // Given: User with GENERAL_MANAGER role in same company
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setUser(testUser);
        csu.setCompany(testCompany);
        csu.setRole(CompanyRole.GENERAL_MANAGER);
        
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.singletonList(csu));
        when(warehouseInventoryService.getUserCompanyRole(testUser)).thenReturn(CompanyRole.GENERAL_MANAGER);

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> permissions = (Map<String, Object>) body.get("permissions");
        assertTrue((Boolean) permissions.get("canRead"));
        assertTrue((Boolean) permissions.get("canWrite"));
        assertTrue((Boolean) permissions.get("canAddInventory"));
        assertTrue((Boolean) permissions.get("canWithdrawInventory"));
    }

    @Test
    void testCheckWarehousePermissions_StoreManager_HasWritePermissions() {
        // Given: User with STORE_MANAGER role in same company
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setUser(testUser);
        csu.setCompany(testCompany);
        csu.setRole(CompanyRole.STORE_MANAGER);
        
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.singletonList(csu));
        when(warehouseInventoryService.getUserCompanyRole(testUser)).thenReturn(CompanyRole.STORE_MANAGER);

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> permissions = (Map<String, Object>) body.get("permissions");
        assertTrue((Boolean) permissions.get("canRead"));
        assertTrue((Boolean) permissions.get("canWrite"));
        assertTrue((Boolean) permissions.get("canAddInventory"));
        assertTrue((Boolean) permissions.get("canWithdrawInventory"));
    }

    @Test
    void testCheckWarehousePermissions_Employee_HasReadOnlyPermissions() {
        // Given: User with EMPLOYEE role in same company
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setUser(testUser);
        csu.setCompany(testCompany);
        csu.setRole(CompanyRole.EMPLOYEE);
        
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.singletonList(csu));
        when(warehouseInventoryService.getUserCompanyRole(testUser)).thenReturn(CompanyRole.EMPLOYEE);

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> permissions = (Map<String, Object>) body.get("permissions");
        assertTrue((Boolean) permissions.get("canRead"));
        assertFalse((Boolean) permissions.get("canWrite"));
        assertFalse((Boolean) permissions.get("canAddInventory"));
        assertFalse((Boolean) permissions.get("canWithdrawInventory"));
    }

    @Test
    void testCheckWarehousePermissions_DifferentCompany_NoPermissions() {
        // Given: User in different company
        Company differentCompany = new Company();
        differentCompany.setId(UUID.randomUUID());
        differentCompany.setName("Different Company");
        
        CompanyStoreUser csu = new CompanyStoreUser();
        csu.setUser(testUser);
        csu.setCompany(differentCompany);
        csu.setRole(CompanyRole.STORE_MANAGER);
        
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(testWarehouse));
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(Collections.singletonList(csu));
        when(warehouseInventoryService.getUserCompanyRole(testUser)).thenReturn(CompanyRole.EMPLOYEE);

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> permissions = (Map<String, Object>) body.get("permissions");
        assertFalse((Boolean) permissions.get("canRead"));
        assertFalse((Boolean) permissions.get("canWrite"));
        assertFalse((Boolean) permissions.get("canAddInventory"));
        assertFalse((Boolean) permissions.get("canWithdrawInventory"));
    }

    @Test
    void testCheckWarehousePermissions_WarehouseNotFound_ReturnsError() {
        // Given: Warehouse does not exist
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = controller.checkWarehousePermissions(warehouseId, authentication);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertNotNull(body.get("error"));
    }
}
