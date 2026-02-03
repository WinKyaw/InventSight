package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.TransferRequest;
import com.pos.inventsight.model.sql.TransferRequestStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferPermissionServiceTest {

    private TransferPermissionService permissionService;
    private User ownerUser;
    private User adminUser;
    private User managerUser;
    private User employeeUser;
    private TransferRequest pendingTransfer;
    private TransferRequest approvedTransfer;
    private TransferRequest readyTransfer;
    private TransferRequest inTransitTransfer;
    private TransferRequest deliveredTransfer;

    @BeforeEach
    void setUp() {
        permissionService = new TransferPermissionService();

        // Create test users
        ownerUser = new User();
        ownerUser.setId(UUID.randomUUID());
        ownerUser.setRole(UserRole.OWNER);
        ownerUser.setUsername("owner");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setUsername("admin");

        managerUser = new User();
        managerUser.setId(UUID.randomUUID());
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setUsername("manager");

        employeeUser = new User();
        employeeUser.setId(UUID.randomUUID());
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setUsername("employee");

        // Create test transfers
        UUID fromLocationId = UUID.randomUUID();
        UUID toLocationId = UUID.randomUUID();

        pendingTransfer = new TransferRequest();
        pendingTransfer.setId(UUID.randomUUID());
        pendingTransfer.setStatus(TransferRequestStatus.PENDING);
        pendingTransfer.setRequestedByUserId(employeeUser.getId());
        pendingTransfer.setFromLocationId(fromLocationId);
        pendingTransfer.setFromLocationType("STORE");
        pendingTransfer.setToLocationId(toLocationId);
        pendingTransfer.setToLocationType("WAREHOUSE");

        approvedTransfer = new TransferRequest();
        approvedTransfer.setId(UUID.randomUUID());
        approvedTransfer.setStatus(TransferRequestStatus.APPROVED);
        approvedTransfer.setRequestedByUserId(employeeUser.getId());
        approvedTransfer.setFromLocationId(fromLocationId);
        approvedTransfer.setFromLocationType("STORE");
        approvedTransfer.setToLocationId(toLocationId);
        approvedTransfer.setToLocationType("WAREHOUSE");

        readyTransfer = new TransferRequest();
        readyTransfer.setId(UUID.randomUUID());
        readyTransfer.setStatus(TransferRequestStatus.READY);
        readyTransfer.setFromLocationId(fromLocationId);
        readyTransfer.setFromLocationType("STORE");
        readyTransfer.setToLocationId(toLocationId);
        readyTransfer.setToLocationType("WAREHOUSE");

        inTransitTransfer = new TransferRequest();
        inTransitTransfer.setId(UUID.randomUUID());
        inTransitTransfer.setStatus(TransferRequestStatus.IN_TRANSIT);
        inTransitTransfer.setFromLocationId(fromLocationId);
        inTransitTransfer.setFromLocationType("STORE");
        inTransitTransfer.setToLocationId(toLocationId);
        inTransitTransfer.setToLocationType("WAREHOUSE");

        deliveredTransfer = new TransferRequest();
        deliveredTransfer.setId(UUID.randomUUID());
        deliveredTransfer.setStatus(TransferRequestStatus.DELIVERED);
        deliveredTransfer.setFromLocationId(fromLocationId);
        deliveredTransfer.setFromLocationType("STORE");
        deliveredTransfer.setToLocationId(toLocationId);
        deliveredTransfer.setToLocationType("WAREHOUSE");
    }

    @Test
    @DisplayName("Should return empty actions when user is null")
    void testNullUser() {
        List<String> actions = permissionService.getAvailableActions(pendingTransfer, null);
        assertTrue(actions.isEmpty());
    }

    @Test
    @DisplayName("Should return empty actions when transfer is null")
    void testNullTransfer() {
        List<String> actions = permissionService.getAvailableActions(null, ownerUser);
        assertTrue(actions.isEmpty());
    }

    @Test
    @DisplayName("OWNER can approve and reject PENDING transfers")
    void testOwnerCanApproveRejectPending() {
        List<String> actions = permissionService.getAvailableActions(pendingTransfer, ownerUser);
        assertTrue(actions.contains("approve"));
        assertTrue(actions.contains("reject"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("ADMIN can approve and reject PENDING transfers")
    void testAdminCanApproveRejectPending() {
        List<String> actions = permissionService.getAvailableActions(pendingTransfer, adminUser);
        assertTrue(actions.contains("approve"));
        assertTrue(actions.contains("reject"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("MANAGER can approve and reject PENDING transfers")
    void testManagerCanApproveRejectPending() {
        List<String> actions = permissionService.getAvailableActions(pendingTransfer, managerUser);
        assertTrue(actions.contains("approve"));
        assertTrue(actions.contains("reject"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("Requester can cancel their own PENDING transfer")
    void testRequesterCanCancelOwnPending() {
        List<String> actions = permissionService.getAvailableActions(pendingTransfer, employeeUser);
        assertTrue(actions.contains("cancel"));
        assertFalse(actions.contains("approve"));
        assertFalse(actions.contains("reject"));
    }

    @Test
    @DisplayName("GM+ can mark APPROVED transfer as ready")
    void testGMCanMarkReadyApproved() {
        List<String> actions = permissionService.getAvailableActions(approvedTransfer, ownerUser);
        assertTrue(actions.contains("markReady"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("GM+ can start delivery on READY transfer")
    void testGMCanStartDeliveryReady() {
        List<String> actions = permissionService.getAvailableActions(readyTransfer, managerUser);
        assertTrue(actions.contains("startDelivery"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("GM+ can mark IN_TRANSIT transfer as delivered")
    void testGMCanMarkDeliveredInTransit() {
        List<String> actions = permissionService.getAvailableActions(inTransitTransfer, ownerUser);
        assertTrue(actions.contains("markDelivered"));
        assertTrue(actions.contains("cancel"));
    }

    @Test
    @DisplayName("GM+ can receive DELIVERED transfer and still cancel if needed")
    void testGMCanReceiveDelivered() {
        List<String> actions = permissionService.getAvailableActions(deliveredTransfer, adminUser);
        assertTrue(actions.contains("receive"));
        assertTrue(actions.contains("cancel")); // GM+ can still cancel DELIVERED transfers
    }

    @Test
    @DisplayName("canPerformAction returns true for allowed action")
    void testCanPerformActionAllowed() {
        boolean canApprove = permissionService.canPerformAction(pendingTransfer, ownerUser, "approve");
        assertTrue(canApprove);
    }

    @Test
    @DisplayName("canPerformAction returns false for disallowed action")
    void testCanPerformActionDisallowed() {
        boolean canApprove = permissionService.canPerformAction(pendingTransfer, employeeUser, "approve");
        assertFalse(canApprove);
    }

    @Test
    @DisplayName("GM+ cannot cancel COMPLETED transfer")
    void testCannotCancelCompleted() {
        TransferRequest completed = new TransferRequest();
        completed.setId(UUID.randomUUID());
        completed.setStatus(TransferRequestStatus.COMPLETED);
        completed.setFromLocationId(UUID.randomUUID());
        completed.setFromLocationType("STORE");
        completed.setToLocationId(UUID.randomUUID());
        completed.setToLocationType("WAREHOUSE");

        List<String> actions = permissionService.getAvailableActions(completed, ownerUser);
        assertFalse(actions.contains("cancel"));
    }

    @Test
    @DisplayName("GM+ cannot cancel CANCELLED transfer")
    void testCannotCancelCancelled() {
        TransferRequest cancelled = new TransferRequest();
        cancelled.setId(UUID.randomUUID());
        cancelled.setStatus(TransferRequestStatus.CANCELLED);
        cancelled.setFromLocationId(UUID.randomUUID());
        cancelled.setFromLocationType("STORE");
        cancelled.setToLocationId(UUID.randomUUID());
        cancelled.setToLocationType("WAREHOUSE");

        List<String> actions = permissionService.getAvailableActions(cancelled, ownerUser);
        assertFalse(actions.contains("cancel"));
    }

    @Test
    @DisplayName("GM+ cannot cancel REJECTED transfer")
    void testCannotCancelRejected() {
        TransferRequest rejected = new TransferRequest();
        rejected.setId(UUID.randomUUID());
        rejected.setStatus(TransferRequestStatus.REJECTED);
        rejected.setFromLocationId(UUID.randomUUID());
        rejected.setFromLocationType("STORE");
        rejected.setToLocationId(UUID.randomUUID());
        rejected.setToLocationType("WAREHOUSE");

        List<String> actions = permissionService.getAvailableActions(rejected, ownerUser);
        assertFalse(actions.contains("cancel"));
    }

    @Test
    @DisplayName("Employee cannot perform GM+ actions")
    void testEmployeeCannotPerformGMActions() {
        List<String> pendingActions = permissionService.getAvailableActions(pendingTransfer, employeeUser);
        assertFalse(pendingActions.contains("approve"));
        assertFalse(pendingActions.contains("reject"));

        List<String> approvedActions = permissionService.getAvailableActions(approvedTransfer, employeeUser);
        assertFalse(approvedActions.contains("markReady"));
    }
}
