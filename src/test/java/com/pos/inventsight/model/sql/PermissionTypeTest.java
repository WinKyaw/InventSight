package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PermissionType enum
 */
public class PermissionTypeTest {
    
    /**
     * Test that MANAGE_SUPPLY permission type exists and has correct properties
     */
    @Test
    public void testManageSupplyPermissionExists() {
        // Verify that MANAGE_SUPPLY enum constant exists
        PermissionType manageSupply = PermissionType.MANAGE_SUPPLY;
        
        // Verify the enum value is not null
        assertNotNull(manageSupply, "MANAGE_SUPPLY permission type should exist");
        
        // Verify it has the correct display name
        assertEquals("Manage Supply Permission", manageSupply.getDisplayName(), 
            "MANAGE_SUPPLY should have correct display name");
        
        // Verify it can be converted from string (as the API endpoint does)
        PermissionType fromString = PermissionType.valueOf("MANAGE_SUPPLY");
        assertEquals(PermissionType.MANAGE_SUPPLY, fromString, 
            "MANAGE_SUPPLY should be parseable from string");
    }
    
    /**
     * Test that all existing permission types still exist
     */
    @Test
    public void testAllPermissionTypesExist() {
        // Verify all expected permission types exist
        assertNotNull(PermissionType.ADD_ITEM);
        assertNotNull(PermissionType.EDIT_ITEM);
        assertNotNull(PermissionType.DELETE_ITEM);
        assertNotNull(PermissionType.MANAGE_SUPPLY);
        
        // Verify there are exactly 4 permission types
        assertEquals(4, PermissionType.values().length, 
            "There should be exactly 4 permission types");
    }
    
    /**
     * Test that each permission type has a display name
     */
    @Test
    public void testAllPermissionTypesHaveDisplayNames() {
        for (PermissionType type : PermissionType.values()) {
            assertNotNull(type.getDisplayName(), 
                type.name() + " should have a display name");
            assertFalse(type.getDisplayName().isEmpty(), 
                type.name() + " display name should not be empty");
        }
    }
}
