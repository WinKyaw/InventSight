package com.pos.inventsight.controller;

import com.pos.inventsight.dto.WarehouseInventoryAdditionRequest;
import com.pos.inventsight.dto.WarehouseInventoryRequest;
import com.pos.inventsight.dto.WarehouseInventoryWithdrawalRequest;
import com.pos.inventsight.security.RoleConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify RBAC (Role-Based Access Control) fixes on WarehouseInventoryController
 * This test verifies that the correct @PreAuthorize annotations are present on the endpoints
 * and that they include the OWNER authority.
 */
public class WarehouseInventoryControllerRbacTest {

    /**
     * Test that POST /warehouse-inventory has correct @PreAuthorize annotation
     * Should use RoleConstants.GM_PLUS which includes OWNER
     */
    @Test
    public void testCreateOrUpdateInventory_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("createOrUpdateInventory",
                WarehouseInventoryRequest.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "POST /warehouse-inventory should have @PreAuthorize annotation");
        assertEquals(RoleConstants.GM_PLUS, preAuthorize.value(),
            "POST /warehouse-inventory should use RoleConstants.GM_PLUS");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.GM_PLUS.contains("OWNER"), 
            "RoleConstants.GM_PLUS should include OWNER authority");
        assertTrue(RoleConstants.GM_PLUS.contains("owner"), 
            "RoleConstants.GM_PLUS should include owner (lowercase) for case-insensitive matching");
    }

    /**
     * Test that POST /warehouse-inventory/add has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_MODIFY_INVENTORY which includes OWNER
     */
    @Test
    public void testAddInventory_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("addInventory",
                WarehouseInventoryAdditionRequest.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "POST /warehouse-inventory/add should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_MODIFY_INVENTORY, preAuthorize.value(),
            "POST /warehouse-inventory/add should use RoleConstants.CAN_MODIFY_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_MODIFY_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_MODIFY_INVENTORY should include OWNER authority");
    }

    /**
     * Test that POST /warehouse-inventory/withdraw has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_WITHDRAW_INVENTORY which includes OWNER
     */
    @Test
    public void testWithdrawInventory_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("withdrawInventory",
                WarehouseInventoryWithdrawalRequest.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "POST /warehouse-inventory/withdraw should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_WITHDRAW_INVENTORY, preAuthorize.value(),
            "POST /warehouse-inventory/withdraw should use RoleConstants.CAN_WITHDRAW_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_WITHDRAW_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_WITHDRAW_INVENTORY should include OWNER authority");
    }

    /**
     * Test that POST /warehouse-inventory/reserve has correct @PreAuthorize annotation
     * Should use RoleConstants.MANAGEMENT which includes OWNER
     */
    @Test
    public void testReserveInventory_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("reserveInventory",
                UUID.class, UUID.class, Integer.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "POST /warehouse-inventory/reserve should have @PreAuthorize annotation");
        assertEquals(RoleConstants.MANAGEMENT, preAuthorize.value(),
            "POST /warehouse-inventory/reserve should use RoleConstants.MANAGEMENT");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.MANAGEMENT.contains("OWNER"), 
            "RoleConstants.MANAGEMENT should include OWNER authority");
    }

    /**
     * Test that GET /warehouse-inventory/warehouse/{warehouseId} has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_VIEW_INVENTORY which includes OWNER
     */
    @Test
    public void testGetWarehouseInventory_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("getWarehouseInventory", 
            UUID.class, int.class, int.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /warehouse-inventory/warehouse/{id} should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_VIEW_INVENTORY, preAuthorize.value(),
            "GET /warehouse-inventory/warehouse/{id} should use RoleConstants.CAN_VIEW_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_VIEW_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_VIEW_INVENTORY should include OWNER authority");
    }

    /**
     * Test that GET /warehouse-inventory/warehouse/{warehouseId}/additions has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_VIEW_INVENTORY which includes OWNER
     */
    @Test
    public void testListAdditions_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("listAdditions",
                UUID.class, LocalDate.class, LocalDate.class, String.class, int.class, int.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /warehouse-inventory/warehouse/{id}/additions should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_VIEW_INVENTORY, preAuthorize.value(),
            "GET /warehouse-inventory/warehouse/{id}/additions should use RoleConstants.CAN_VIEW_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_VIEW_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_VIEW_INVENTORY should include OWNER authority");
    }

    /**
     * Test that GET /warehouse-inventory/warehouse/{warehouseId}/withdrawals has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_VIEW_INVENTORY which includes OWNER
     */
    @Test
    public void testListWithdrawals_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("listWithdrawals",
                UUID.class, LocalDate.class, LocalDate.class, String.class, int.class, int.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /warehouse-inventory/warehouse/{id}/withdrawals should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_VIEW_INVENTORY, preAuthorize.value(),
            "GET /warehouse-inventory/warehouse/{id}/withdrawals should use RoleConstants.CAN_VIEW_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_VIEW_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_VIEW_INVENTORY should include OWNER authority");
    }

    /**
     * Test that GET /warehouse-inventory/warehouse/{warehouseId}/value has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_VIEW_INVENTORY which includes OWNER
     */
    @Test
    public void testGetInventoryValue_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseInventoryController.class.getMethod("getInventoryValue", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "GET /warehouse-inventory/warehouse/{id}/value should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_VIEW_INVENTORY, preAuthorize.value(),
            "GET /warehouse-inventory/warehouse/{id}/value should use RoleConstants.CAN_VIEW_INVENTORY");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_VIEW_INVENTORY.contains("OWNER"), 
            "RoleConstants.CAN_VIEW_INVENTORY should include OWNER authority");
    }

    /**
     * Verify that RoleConstants contains case-insensitive role matching
     */
    @Test
    public void testRoleConstants_CaseInsensitive() {
        // Verify ALL_ROLES includes both uppercase and lowercase variants
        assertTrue(RoleConstants.ALL_ROLES.contains("'OWNER'"), "ALL_ROLES should include 'OWNER'");
        assertTrue(RoleConstants.ALL_ROLES.contains("'owner'"), "ALL_ROLES should include 'owner'");
        assertTrue(RoleConstants.ALL_ROLES.contains("'FOUNDER'"), "ALL_ROLES should include 'FOUNDER'");
        assertTrue(RoleConstants.ALL_ROLES.contains("'founder'"), "ALL_ROLES should include 'founder'");
        
        // Verify GM_PLUS includes both uppercase and lowercase variants
        assertTrue(RoleConstants.GM_PLUS.contains("'OWNER'"), "GM_PLUS should include 'OWNER'");
        assertTrue(RoleConstants.GM_PLUS.contains("'owner'"), "GM_PLUS should include 'owner'");
        assertTrue(RoleConstants.GM_PLUS.contains("'ADMIN'"), "GM_PLUS should include 'ADMIN'");
        assertTrue(RoleConstants.GM_PLUS.contains("'admin'"), "GM_PLUS should include 'admin'");
    }
}
