package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.WarehousePermissionRepository;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for new warehouse permission assignment endpoints
 */
public class WarehousePermissionAssignmentTest {

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private UserService userService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehousePermissionRepository warehousePermissionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WarehouseInventoryController controller;

    private UUID warehouseId;
    private UUID userId;
    private UUID targetUserId;
    private User currentUser;
    private User targetUser;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        warehouseId = UUID.randomUUID();
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        // Setup current user (GM)
        currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("gmuser");
        currentUser.setEmail("gm@example.com");
        currentUser.setRole(UserRole.MANAGER);

        // Setup target user
        targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setUsername("testuser");
        targetUser.setEmail("test@example.com");
        targetUser.setRole(UserRole.EMPLOYEE);

        // Setup warehouse
        warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Test Warehouse");
        warehouse.setLocation("Test Location");

        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(currentUser);
    }

    @Test
    void testGetWarehouseUsers_GMPlus_Success() {
        // Given: GM user trying to get warehouse users
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        WarehousePermission permission = new WarehousePermission(warehouse, targetUser, WarehousePermission.PermissionType.READ_WRITE);
        permission.setGrantedBy("admin");
        permission.setGrantedAt(LocalDateTime.now());

        when(warehousePermissionRepository.findByWarehouseIdAndIsActive(warehouseId, true))
            .thenReturn(Collections.singletonList(permission));

        // When
        ResponseEntity<?> response = controller.getWarehouseUsers(warehouseId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(warehouseId, body.get("warehouseId"));
        assertEquals("Test Warehouse", body.get("warehouseName"));
        
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(1, users.size());
        assertEquals(targetUserId, users.get(0).get("userId"));
        assertEquals("testuser", users.get(0).get("username"));
        assertEquals("READ_WRITE", users.get(0).get("permission"));
    }

    @Test
    void testGetWarehouseUsers_NonGMPlus_Forbidden() {
        // Given: Non-GM user trying to get warehouse users
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);

        // When
        ResponseEntity<?> response = controller.getWarehouseUsers(warehouseId, authentication);

        // Then
        assertEquals(403, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("error").toString().contains("Insufficient permissions"));
    }

    @Test
    void testGrantWarehousePermission_GMPlus_Success() {
        // Given: GM user granting permission
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(userService.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, targetUserId, true))
            .thenReturn(Optional.empty());

        WarehousePermission savedPermission = new WarehousePermission(warehouse, targetUser, WarehousePermission.PermissionType.READ);
        savedPermission.setId(UUID.randomUUID());
        when(warehousePermissionRepository.save(any(WarehousePermission.class))).thenReturn(savedPermission);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", targetUserId.toString());
        request.put("permissionType", "READ");

        // When
        ResponseEntity<?> response = controller.grantWarehousePermission(warehouseId, request, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(warehouseId, body.get("warehouseId"));
        assertEquals(targetUserId.toString(), body.get("userId"));
        assertEquals("READ", body.get("permissionType"));
        verify(warehousePermissionRepository, times(1)).save(any(WarehousePermission.class));
    }

    @Test
    void testGrantWarehousePermission_UpdateExisting_Success() {
        // Given: Permission already exists, updating it
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(userService.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        WarehousePermission existingPermission = new WarehousePermission(warehouse, targetUser, WarehousePermission.PermissionType.READ);
        existingPermission.setId(UUID.randomUUID());
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, targetUserId, true))
            .thenReturn(Optional.of(existingPermission));
        when(warehousePermissionRepository.save(any(WarehousePermission.class))).thenReturn(existingPermission);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", targetUserId.toString());
        request.put("permissionType", "READ_WRITE");

        // When
        ResponseEntity<?> response = controller.grantWarehousePermission(warehouseId, request, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("READ_WRITE", body.get("permissionType"));
        verify(warehousePermissionRepository, times(1)).save(any(WarehousePermission.class));
    }

    @Test
    void testGrantWarehousePermission_NonGMPlus_Forbidden() {
        // Given: Non-GM user trying to grant permission
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", targetUserId.toString());
        request.put("permissionType", "READ");

        // When
        ResponseEntity<?> response = controller.grantWarehousePermission(warehouseId, request, authentication);

        // Then
        assertEquals(403, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testGrantWarehousePermission_InvalidPermissionType_BadRequest() {
        // Given: Invalid permission type
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(userService.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        Map<String, Object> request = new HashMap<>();
        request.put("userId", targetUserId.toString());
        request.put("permissionType", "INVALID_TYPE");

        // When
        ResponseEntity<?> response = controller.grantWarehousePermission(warehouseId, request, authentication);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("error").toString().contains("Invalid permission type"));
    }

    @Test
    void testRevokeWarehousePermission_GMPlus_Success() {
        // Given: GM user revoking permission
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);

        WarehousePermission permission = new WarehousePermission(warehouse, targetUser, WarehousePermission.PermissionType.READ);
        permission.setId(UUID.randomUUID());
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, targetUserId, true))
            .thenReturn(Optional.of(permission));
        when(warehousePermissionRepository.save(any(WarehousePermission.class))).thenReturn(permission);

        // When
        ResponseEntity<?> response = controller.revokeWarehousePermission(warehouseId, targetUserId, authentication);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("revoked successfully"));
        verify(warehousePermissionRepository, times(1)).save(any(WarehousePermission.class));
    }

    @Test
    void testRevokeWarehousePermission_NonGMPlus_Forbidden() {
        // Given: Non-GM user trying to revoke permission
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);

        // When
        ResponseEntity<?> response = controller.revokeWarehousePermission(warehouseId, targetUserId, authentication);

        // Then
        assertEquals(403, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testRevokeWarehousePermission_PermissionNotFound_NotFound() {
        // Given: Permission doesn't exist
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, targetUserId, true))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = controller.revokeWarehousePermission(warehouseId, targetUserId, authentication);

        // Then
        assertEquals(404, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("error").toString().contains("No active permission found"));
    }

    @Test
    void testCheckWarehousePermissions_WithExplicitPermission_Success() {
        // Given: User has explicit READ permission
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);

        WarehousePermission permission = new WarehousePermission(warehouse, currentUser, WarehousePermission.PermissionType.READ);
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, userId, true))
            .thenReturn(Optional.of(permission));

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
        assertFalse((Boolean) permissions.get("isGMPlus"));
    }

    @Test
    void testCheckWarehousePermissions_GMPlus_FullAccess() {
        // Given: GM+ user
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);

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
        assertTrue((Boolean) permissions.get("isGMPlus"));
    }

    @Test
    void testCheckWarehousePermissions_NoPermission_NoAccess() {
        // Given: User has no permission
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.EMPLOYEE);
        when(warehousePermissionRepository.findByWarehouseIdAndUserIdAndIsActive(warehouseId, userId, true))
            .thenReturn(Optional.empty());

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
        assertFalse((Boolean) permissions.get("isGMPlus"));
    }
}
