package com.pos.inventsight.constants;

import com.pos.inventsight.model.sql.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RoleConstants utility class
 */
class RoleConstantsTest {
    
    @Test
    void testIsGMPlus_WithOwner_ReturnsTrue() {
        assertTrue(RoleConstants.isGMPlus(UserRole.OWNER));
    }
    
    @Test
    void testIsGMPlus_WithFounder_ReturnsTrue() {
        assertTrue(RoleConstants.isGMPlus(UserRole.FOUNDER));
    }
    
    @Test
    void testIsGMPlus_WithCoOwner_ReturnsTrue() {
        assertTrue(RoleConstants.isGMPlus(UserRole.CO_OWNER));
    }
    
    @Test
    void testIsGMPlus_WithManager_ReturnsTrue() {
        assertTrue(RoleConstants.isGMPlus(UserRole.MANAGER));
    }
    
    @Test
    void testIsGMPlus_WithAdmin_ReturnsTrue() {
        assertTrue(RoleConstants.isGMPlus(UserRole.ADMIN));
    }
    
    @Test
    void testIsGMPlus_WithEmployee_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.EMPLOYEE));
    }
    
    @Test
    void testIsGMPlus_WithCashier_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.CASHIER));
    }
    
    @Test
    void testIsGMPlus_WithCustomer_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.CUSTOMER));
    }
    
    @Test
    void testIsGMPlus_WithMerchant_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.MERCHANT));
    }
    
    @Test
    void testIsGMPlus_WithPartner_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.PARTNER));
    }
    
    @Test
    void testIsGMPlus_WithUser_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(UserRole.USER));
    }
    
    @Test
    void testIsGMPlus_WithNull_ReturnsFalse() {
        assertFalse(RoleConstants.isGMPlus(null));
    }
    
    @Test
    void testGMPlusRolesList_ContainsAllExpectedRoles() {
        assertEquals(5, RoleConstants.GM_PLUS_ROLES.size());
        assertTrue(RoleConstants.GM_PLUS_ROLES.contains(UserRole.OWNER));
        assertTrue(RoleConstants.GM_PLUS_ROLES.contains(UserRole.FOUNDER));
        assertTrue(RoleConstants.GM_PLUS_ROLES.contains(UserRole.CO_OWNER));
        assertTrue(RoleConstants.GM_PLUS_ROLES.contains(UserRole.MANAGER));
        assertTrue(RoleConstants.GM_PLUS_ROLES.contains(UserRole.ADMIN));
    }
    
    @Test
    void testGetGMPlusRolesForPreAuthorize_ReturnsCorrectString() {
        String expected = "hasAnyRole('OWNER', 'FOUNDER', 'CO_OWNER', 'MANAGER', 'ADMIN')";
        assertEquals(expected, RoleConstants.getGMPlusRolesForPreAuthorize());
    }
}
