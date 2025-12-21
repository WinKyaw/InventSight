package com.pos.inventsight.controller;

import com.pos.inventsight.dto.WarehouseRequest;
import com.pos.inventsight.security.RoleConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify RBAC (Role-Based Access Control) fixes on WarehouseController
 * This test verifies that the correct @PreAuthorize annotations are present on the endpoints
 * and that they include the OWNER authority.
 */
public class WarehouseControllerRbacTest {

    /**
     * Test that POST /warehouses has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_MANAGE_WAREHOUSES which includes OWNER
     */
    @Test
    public void testCreateWarehouse_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("createWarehouse",
                WarehouseRequest.class, BindingResult.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "POST /warehouses should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_MANAGE_WAREHOUSES, preAuthorize.value(),
            "POST /warehouses should use RoleConstants.CAN_MANAGE_WAREHOUSES");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_MANAGE_WAREHOUSES.contains("OWNER"), 
            "RoleConstants.CAN_MANAGE_WAREHOUSES should include OWNER authority");
        assertTrue(RoleConstants.CAN_MANAGE_WAREHOUSES.contains("owner"), 
            "RoleConstants.CAN_MANAGE_WAREHOUSES should include owner (lowercase) for case-insensitive matching");
    }

    /**
     * Test that PUT /warehouses/{id} has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_MANAGE_WAREHOUSES which includes OWNER
     */
    @Test
    public void testUpdateWarehouse_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("updateWarehouse",
                UUID.class, WarehouseRequest.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "PUT /warehouses/{id} should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_MANAGE_WAREHOUSES, preAuthorize.value(),
            "PUT /warehouses/{id} should use RoleConstants.CAN_MANAGE_WAREHOUSES");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_MANAGE_WAREHOUSES.contains("OWNER"), 
            "RoleConstants.CAN_MANAGE_WAREHOUSES should include OWNER authority");
    }

    /**
     * Test that DELETE /warehouses/{id} has correct @PreAuthorize annotation
     * Should use RoleConstants.CAN_MANAGE_WAREHOUSES which includes OWNER
     */
    @Test
    public void testDeleteWarehouse_HasCorrectPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = WarehouseController.class.getMethod("deleteWarehouse",
                UUID.class, Authentication.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        
        assertNotNull(preAuthorize, "DELETE /warehouses/{id} should have @PreAuthorize annotation");
        assertEquals(RoleConstants.CAN_MANAGE_WAREHOUSES, preAuthorize.value(),
            "DELETE /warehouses/{id} should use RoleConstants.CAN_MANAGE_WAREHOUSES");
        
        // Verify OWNER is included in the expanded constant
        assertTrue(RoleConstants.CAN_MANAGE_WAREHOUSES.contains("OWNER"), 
            "RoleConstants.CAN_MANAGE_WAREHOUSES should include OWNER authority");
    }

    /**
     * Verify that CAN_MANAGE_WAREHOUSES equals GM_PLUS
     */
    @Test
    public void testCanManageWarehouses_EqualsGMPlus() {
        assertEquals(RoleConstants.GM_PLUS, RoleConstants.CAN_MANAGE_WAREHOUSES,
            "CAN_MANAGE_WAREHOUSES should be the same as GM_PLUS");
    }
}
