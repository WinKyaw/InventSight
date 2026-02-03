package com.pos.inventsight.controller;

import com.pos.inventsight.dto.TransferApprovalRequest;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.TransferPermissionService;
import com.pos.inventsight.service.TransferRequestService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransferRequestController to verify availableActions
 * are properly included in API responses
 */
class TransferRequestAvailableActionsControllerTest {

    @Mock
    private TransferRequestService transferRequestService;

    @Mock
    private TransferPermissionService transferPermissionService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferRequestController controller;

    private User ownerUser;
    private User employeeUser;
    private TransferRequest pendingTransfer;
    private TransferRequest approvedTransfer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        ownerUser = new User();
        ownerUser.setId(UUID.randomUUID());
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setUsername("owner");

        employeeUser = new User();
        employeeUser.setId(UUID.randomUUID());
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setUsername("employee");

        // Create test transfers
        pendingTransfer = new TransferRequest();
        pendingTransfer.setId(UUID.randomUUID());
        pendingTransfer.setStatus(TransferRequestStatus.PENDING);
        pendingTransfer.setRequestedByUserId(employeeUser.getId());
        pendingTransfer.setFromLocationId(UUID.randomUUID());
        pendingTransfer.setFromLocationType("STORE");
        pendingTransfer.setToLocationId(UUID.randomUUID());
        pendingTransfer.setToLocationType("WAREHOUSE");

        approvedTransfer = new TransferRequest();
        approvedTransfer.setId(UUID.randomUUID());
        approvedTransfer.setStatus(TransferRequestStatus.APPROVED);
        approvedTransfer.setRequestedByUserId(employeeUser.getId());
        approvedTransfer.setFromLocationId(UUID.randomUUID());
        approvedTransfer.setFromLocationType("STORE");
        approvedTransfer.setToLocationId(UUID.randomUUID());
        approvedTransfer.setToLocationType("WAREHOUSE");
    }

    @Test
    @DisplayName("GET /transfers/{id} should include availableActions in response")
    void testGetTransferByIdIncludesAvailableActions() {
        // Arrange
        when(authentication.getName()).thenReturn("owner");
        when(userService.getUserByUsername("owner")).thenReturn(ownerUser);
        when(transferRequestService.getTransferRequestById(pendingTransfer.getId()))
            .thenReturn(pendingTransfer);
        when(transferPermissionService.getAvailableActions(pendingTransfer, ownerUser))
            .thenReturn(Arrays.asList("approve", "reject", "cancel"));

        // Act
        ResponseEntity<?> response = controller.getTransferRequestById(pendingTransfer.getId(), authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNotNull(responseBody.get("request"));
        assertNotNull(responseBody.get("availableActions"));
        
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) responseBody.get("availableActions");
        assertTrue(actions.contains("approve"));
        assertTrue(actions.contains("reject"));
        assertTrue(actions.contains("cancel"));
        
        // Verify interactions
        verify(transferPermissionService).getAvailableActions(pendingTransfer, ownerUser);
    }

    @Test
    @DisplayName("PUT /transfers/{id}/approve should validate permissions and return availableActions")
    void testApproveValidatesPermissionsAndReturnsActions() {
        // Arrange
        TransferApprovalRequest approvalRequest = new TransferApprovalRequest();
        approvalRequest.setApprovedQuantity(10);
        approvalRequest.setNotes("Approved");

        when(authentication.getName()).thenReturn("owner");
        when(userService.getUserByUsername("owner")).thenReturn(ownerUser);
        when(transferRequestService.getTransferRequestById(pendingTransfer.getId()))
            .thenReturn(pendingTransfer);
        when(transferPermissionService.canPerformAction(pendingTransfer, ownerUser, "approve"))
            .thenReturn(true);
        when(transferRequestService.approveTransferRequest(pendingTransfer.getId(), 10, ownerUser))
            .thenReturn(approvedTransfer);
        when(transferPermissionService.getAvailableActions(approvedTransfer, ownerUser))
            .thenReturn(Arrays.asList("markReady", "cancel"));

        // Act
        ResponseEntity<?> response = controller.approveTransferRequest(
            pendingTransfer.getId(), approvalRequest, authentication);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNotNull(responseBody.get("request"));
        assertNotNull(responseBody.get("availableActions"));
        
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) responseBody.get("availableActions");
        assertTrue(actions.contains("markReady"));
        assertTrue(actions.contains("cancel"));
        
        // Verify permission check was performed
        verify(transferPermissionService).canPerformAction(pendingTransfer, ownerUser, "approve");
        verify(transferPermissionService).getAvailableActions(approvedTransfer, ownerUser);
    }

    @Test
    @DisplayName("PUT /transfers/{id}/approve should reject when user lacks permission")
    void testApproveDeniesWhenNoPermission() {
        // Arrange
        TransferApprovalRequest approvalRequest = new TransferApprovalRequest();
        approvalRequest.setApprovedQuantity(10);

        when(authentication.getName()).thenReturn("employee");
        when(userService.getUserByUsername("employee")).thenReturn(employeeUser);
        when(transferRequestService.getTransferRequestById(pendingTransfer.getId()))
            .thenReturn(pendingTransfer);
        when(transferPermissionService.canPerformAction(pendingTransfer, employeeUser, "approve"))
            .thenReturn(false);

        // Act
        ResponseEntity<?> response = controller.approveTransferRequest(
            pendingTransfer.getId(), approvalRequest, authentication);

        // Assert
        assertEquals(403, response.getStatusCodeValue());
        
        // Verify permission check was performed but approval was NOT called
        verify(transferPermissionService).canPerformAction(pendingTransfer, employeeUser, "approve");
        verify(transferRequestService, never()).approveTransferRequest(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Verify response structure includes all required fields")
    void testResponseStructure() {
        // Arrange
        when(authentication.getName()).thenReturn("owner");
        when(userService.getUserByUsername("owner")).thenReturn(ownerUser);
        when(transferRequestService.getTransferRequestById(pendingTransfer.getId()))
            .thenReturn(pendingTransfer);
        when(transferPermissionService.getAvailableActions(pendingTransfer, ownerUser))
            .thenReturn(Arrays.asList("approve", "reject", "cancel"));

        // Act
        ResponseEntity<?> response = controller.getTransferRequestById(pendingTransfer.getId(), authentication);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        // Verify response has exactly the expected structure
        assertTrue(responseBody.containsKey("success"));
        assertTrue(responseBody.containsKey("request"));
        assertTrue(responseBody.containsKey("availableActions"));
        
        // Verify types
        assertEquals(Boolean.class, responseBody.get("success").getClass());
        assertEquals(TransferRequest.class, responseBody.get("request").getClass());
        assertTrue(responseBody.get("availableActions") instanceof List);
    }
}
