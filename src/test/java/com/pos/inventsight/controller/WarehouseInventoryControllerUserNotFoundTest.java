package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.WarehousePermissionRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for WarehouseInventoryController's getUserWarehouseAssignments endpoint
 * Specifically tests the graceful handling of missing users (User Not Found scenario)
 */
public class WarehouseInventoryControllerUserNotFoundTest {

    @Mock
    private UserService userService;

    @Mock
    private WarehousePermissionRepository warehousePermissionRepository;

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WarehouseInventoryController controller;

    private UUID missingUserId;
    private UUID currentUserId;
    private User currentUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        missingUserId = UUID.fromString("605c0e40-7383-4ae3-b37c-033fe7cd7536");
        currentUserId = UUID.randomUUID();
        
        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setUsername("test-gm");
        currentUser.setEmail("gm@test.com");
    }

    /**
     * Test that getUserWarehouseAssignments returns empty list with 200 OK
     * when user doesn't exist (instead of throwing 500 error)
     */
    @Test
    public void testGetUserWarehouseAssignments_UserNotFound_ReturnsEmptyList() {
        // Setup: GM+ user trying to view assignments for missing user
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(userService.findById(missingUserId)).thenReturn(Optional.empty());
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        
        // Execute
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(missingUserId, authentication);
        
        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK, not 500 error");
        
        // Verify response body structure
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"), "Success should be true");
        assertEquals(missingUserId, body.get("userId"), "Should include the userId");
        assertNull(body.get("username"), "Username should be null for missing user");
        assertEquals(0, body.get("count"), "Count should be 0");
        
        // Verify warehouses is an empty list
        Object warehousesObj = body.get("warehouses");
        assertNotNull(warehousesObj);
        assertTrue(warehousesObj instanceof List, "Warehouses should be a list");
        @SuppressWarnings("unchecked")
        List<Object> warehouses = (List<Object>) warehousesObj;
        assertTrue(warehouses.isEmpty(), "Warehouses list should be empty");
        
        // Verify helpful message is included
        assertTrue(body.containsKey("message"), "Should include a message");
        String message = (String) body.get("message");
        assertTrue(message.contains("User account not found"), "Message should mention user not found");
    }

    /**
     * Test that a regular user can view their own (missing) assignments
     */
    @Test
    public void testGetUserWarehouseAssignments_SelfAccess_UserNotFound() {
        // Setup: User trying to view their own assignments, but their user record doesn't exist
        // (edge case: orphaned employee record)
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(userService.findById(currentUserId)).thenReturn(Optional.empty());
        
        // Execute - viewing own assignments
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(currentUserId, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK for self-access");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(currentUserId, body.get("userId"));
    }

    /**
     * Test that when user exists, the endpoint still works correctly
     * (regression test to ensure fix doesn't break existing functionality)
     */
    @Test
    public void testGetUserWarehouseAssignments_UserExists_StillWorks() {
        // Setup: Valid user with no warehouse permissions
        User targetUser = new User();
        targetUser.setId(missingUserId);
        targetUser.setUsername("existing-user");
        targetUser.setEmail("user@test.com");
        
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(userService.findById(missingUserId)).thenReturn(Optional.of(targetUser));
        when(warehouseInventoryService.getUserCompanyRole(currentUser)).thenReturn(CompanyRole.GENERAL_MANAGER);
        when(warehousePermissionRepository.findByUserIdAndIsActive(missingUserId, true))
            .thenReturn(Collections.emptyList());
        
        // Execute
        ResponseEntity<?> response = controller.getUserWarehouseAssignments(missingUserId, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(missingUserId, body.get("userId"));
        assertEquals("existing-user", body.get("username"), "Should include username when user exists");
        assertEquals(0, body.get("count"));
        assertFalse(body.containsKey("message"), "Should not have 'not found' message when user exists");
    }
}
