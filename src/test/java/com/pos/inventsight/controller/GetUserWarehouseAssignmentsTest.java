package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.WarehousePermissionRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for getUserWarehouseAssignments endpoint
 * Tests the new GET /warehouse-inventory/user/{userId}/warehouses endpoint
 */
public class GetUserWarehouseAssignmentsTest {

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private UserService userService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Mock
    private WarehousePermissionRepository warehousePermissionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WarehouseInventoryController controller;

    private UUID userId;
    private UUID otherUserId;
    private UUID warehouseId1;
    private UUID warehouseId2;
    private UUID companyId;
    private User testUser;
    private User otherUser;
    private Company testCompany;
    private Warehouse warehouse1;
    private Warehouse warehouse2;
    private WarehousePermission permission1;
    private WarehousePermission permission2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        warehouseId1 = UUID.randomUUID();
        warehouseId2 = UUID.randomUUID();
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

        // Setup other user
        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.EMPLOYEE);

        // Setup test warehouses
        warehouse1 = new Warehouse();
        warehouse1.setId(warehouseId1);
        warehouse1.setName("Warehouse One");
        warehouse1.setLocation("Location One");
        warehouse1.setAddress("123 Main St");
        warehouse1.setCity("Springfield");
        warehouse1.setState("IL");
        warehouse1.setCountry("USA");
        warehouse1.setCompany(testCompany);

        warehouse2 = new Warehouse();
        warehouse2.setId(warehouseId2);
        warehouse2.setName("Warehouse Two");
        warehouse2.setLocation("Location Two");
        warehouse2.setAddress("456 Center Ave");
        warehouse2.setCity("Springfield");
        warehouse2.setState("IL");
        warehouse2.setCountry("USA");
        warehouse2.setCompany(testCompany);

        // Setup test permissions
        permission1 = new WarehousePermission();
        permission1.setId(UUID.randomUUID());
        permission1.setWarehouse(warehouse1);
        permission1.setUser(testUser);
        permission1.setPermissionType(WarehousePermission.PermissionType.READ_WRITE);
        permission1.setGrantedBy("admin");
        permission1.setGrantedAt(LocalDateTime.now());
        permission1.setIsActive(true);

        permission2 = new WarehousePermission();
        permission2.setId(UUID.randomUUID());
        permission2.setWarehouse(warehouse2);
        permission2.setUser(testUser);
        permission2.setPermissionType(WarehousePermission.PermissionType.READ);
        permission2.setGrantedBy("admin");
        permission2.setGrantedAt(LocalDateTime.now());
        permission2.setIsActive(true);

        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    void testGetUserWarehouseAssignments_UserViewingOwnAssignments_Success() {
        // Given: User requesting their own warehouse assignments
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(permission1, permission2));

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(userId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(userId, responseBody.get("userId"));
        assertEquals("testuser", responseBody.get("username"));
        assertEquals(2, responseBody.get("count"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warehouses = (List<Map<String, Object>>) responseBody.get("warehouses");
        assertEquals(2, warehouses.size());

        // Verify first warehouse assignment
        Map<String, Object> assignment1 = warehouses.get(0);
        assertEquals(warehouseId1, assignment1.get("warehouseId"));
        assertEquals("Warehouse One", assignment1.get("warehouseName"));
        assertEquals("Location One", assignment1.get("warehouseLocation"));
        assertEquals("READ_WRITE", assignment1.get("permissionType"));

        // Verify warehouse details are included
        @SuppressWarnings("unchecked")
        Map<String, Object> warehouseInfo = (Map<String, Object>) assignment1.get("warehouse");
        assertNotNull(warehouseInfo);
        assertEquals("Warehouse One", warehouseInfo.get("name"));
        assertEquals("123 Main St", warehouseInfo.get("address"));
        assertEquals("Springfield", warehouseInfo.get("city"));
    }

    @Test
    void testGetUserWarehouseAssignments_UserViewingOwnAssignments_NoPermissions() {
        // Given: User with no warehouse assignments
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(userId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("count"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warehouses = (List<Map<String, Object>>) responseBody.get("warehouses");
        assertEquals(0, warehouses.size());
    }

    @Test
    void testGetUserWarehouseAssignments_EmployeeViewingOthersAssignments_Forbidden() {
        // Given: Regular employee trying to view another user's assignments
        when(warehouseInventoryService.getUserCompanyRole(testUser))
            .thenReturn(CompanyRole.EMPLOYEE);

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(otherUserId, authentication);

        // Then
        assertEquals(403, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("You can only view your own warehouse assignments", responseBody.get("error"));
    }

    @Test
    void testGetUserWarehouseAssignments_GMViewingOthersAssignments_Success() {
        // Given: GM user viewing another user's assignments
        testUser.setRole(UserRole.MANAGER);
        when(warehouseInventoryService.getUserCompanyRole(testUser))
            .thenReturn(CompanyRole.GENERAL_MANAGER);
        when(userService.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(warehousePermissionRepository.findByUserIdAndIsActive(otherUserId, true))
            .thenReturn(Arrays.asList(permission1));

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(otherUserId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(otherUserId, responseBody.get("userId"));
        assertEquals("otheruser", responseBody.get("username"));
        assertEquals(1, responseBody.get("count"));
    }

    @Test
    void testGetUserWarehouseAssignments_OwnerViewingOthersAssignments_Success() {
        // Given: OWNER user viewing another user's assignments
        testUser.setRole(UserRole.OWNER);
        when(warehouseInventoryService.getUserCompanyRole(testUser))
            .thenReturn(CompanyRole.FOUNDER);
        when(userService.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(warehousePermissionRepository.findByUserIdAndIsActive(otherUserId, true))
            .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(otherUserId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals(0, responseBody.get("count"));
    }

    @Test
    void testGetUserWarehouseAssignments_UserNotFound_Error() {
        // Given: Requesting assignments for a non-existent user
        when(userService.findById(userId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(userId, authentication);

        // Then
        assertEquals(500, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(((String) responseBody.get("error")).contains("User not found"));
    }

    @Test
    void testGetUserWarehouseAssignments_OnlyActivePermissions() {
        // Given: User has both active and inactive permissions
        WarehousePermission inactivePermission = new WarehousePermission();
        inactivePermission.setId(UUID.randomUUID());
        inactivePermission.setWarehouse(warehouse1);
        inactivePermission.setUser(testUser);
        inactivePermission.setPermissionType(WarehousePermission.PermissionType.READ);
        inactivePermission.setIsActive(false); // Inactive

        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        // Repository should only return active permissions
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(permission1)); // Only active one

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(userId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(1, responseBody.get("count")); // Only active permission
        
        // Verify repository was called with isActive = true
        verify(warehousePermissionRepository).findByUserIdAndIsActive(userId, true);
    }

    @Test
    void testGetUserWarehouseAssignments_VerifyResponseFormat() {
        // Given: User with one warehouse assignment
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(warehousePermissionRepository.findByUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(permission1));

        // When
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(userId, authentication);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        // Verify top-level response structure
        assertTrue(responseBody.containsKey("success"));
        assertTrue(responseBody.containsKey("userId"));
        assertTrue(responseBody.containsKey("username"));
        assertTrue(responseBody.containsKey("warehouses"));
        assertTrue(responseBody.containsKey("count"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warehouses = (List<Map<String, Object>>) responseBody.get("warehouses");
        Map<String, Object> assignment = warehouses.get(0);

        // Verify assignment structure
        assertTrue(assignment.containsKey("id"));
        assertTrue(assignment.containsKey("warehouseId"));
        assertTrue(assignment.containsKey("warehouseName"));
        assertTrue(assignment.containsKey("warehouseLocation"));
        assertTrue(assignment.containsKey("permissionType"));
        assertTrue(assignment.containsKey("grantedBy"));
        assertTrue(assignment.containsKey("grantedAt"));
        assertTrue(assignment.containsKey("isActive"));
        assertTrue(assignment.containsKey("warehouse"));

        // Verify warehouse details structure
        @SuppressWarnings("unchecked")
        Map<String, Object> warehouseInfo = (Map<String, Object>) assignment.get("warehouse");
        assertTrue(warehouseInfo.containsKey("id"));
        assertTrue(warehouseInfo.containsKey("name"));
        assertTrue(warehouseInfo.containsKey("location"));
        assertTrue(warehouseInfo.containsKey("address"));
        assertTrue(warehouseInfo.containsKey("city"));
        assertTrue(warehouseInfo.containsKey("state"));
        assertTrue(warehouseInfo.containsKey("country"));
    }
}
