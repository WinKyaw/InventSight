package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserRoleTest {
    
    @Test
    void testAllRolesExist() {
        // Test that all expected roles are present in the enum
        UserRole[] roles = UserRole.values();
        
        // Verify expected count of roles (10 total roles)
        assertEquals(10, roles.length, "UserRole should have 10 roles");
        
        // Verify primary roles exist
        assertNotNull(UserRole.valueOf("OWNER"));
        assertNotNull(UserRole.valueOf("CO_OWNER"));
        assertNotNull(UserRole.valueOf("MANAGER"));
        assertNotNull(UserRole.valueOf("EMPLOYEE"));
        assertNotNull(UserRole.valueOf("CUSTOMER"));
        
        // Verify new roles exist
        assertNotNull(UserRole.valueOf("MERCHANT"));
        assertNotNull(UserRole.valueOf("PARTNER"));
        
        // Verify legacy roles exist
        assertNotNull(UserRole.valueOf("USER"));
        assertNotNull(UserRole.valueOf("ADMIN"));
        assertNotNull(UserRole.valueOf("CASHIER"));
    }
    
    @Test
    void testNewMerchantRoleExists() {
        // Test that MERCHANT role was added successfully
        UserRole merchant = UserRole.MERCHANT;
        assertNotNull(merchant);
        assertEquals("MERCHANT", merchant.name());
    }
    
    @Test
    void testNewPartnerRoleExists() {
        // Test that PARTNER role was added successfully
        UserRole partner = UserRole.PARTNER;
        assertNotNull(partner);
        assertEquals("PARTNER", partner.name());
    }
    
    @Test
    void testRoleNameConversion() {
        // Test that role names can be converted to enum values
        assertEquals(UserRole.MERCHANT, UserRole.valueOf("MERCHANT"));
        assertEquals(UserRole.PARTNER, UserRole.valueOf("PARTNER"));
        assertEquals(UserRole.EMPLOYEE, UserRole.valueOf("EMPLOYEE"));
        assertEquals(UserRole.CUSTOMER, UserRole.valueOf("CUSTOMER"));
    }
}
