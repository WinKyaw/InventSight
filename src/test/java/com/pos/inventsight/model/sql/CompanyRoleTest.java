package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyRoleTest {
    
    @Test
    void testCeoRolePermissions() {
        CompanyRole ceo = CompanyRole.CEO;
        
        assertTrue(ceo.isOwnerLevel());
        assertTrue(ceo.isManagerLevel());
        assertTrue(ceo.canManageStores());
        assertTrue(ceo.canManageUsers());
        assertTrue(ceo.canManageWarehouses());
        assertTrue(ceo.canManagePricing());
        assertEquals("Chief Executive Officer", ceo.getDisplayName());
    }
    
    @Test
    void testFounderRolePermissions() {
        CompanyRole founder = CompanyRole.FOUNDER;
        
        assertTrue(founder.isOwnerLevel());
        assertTrue(founder.isManagerLevel());
        assertTrue(founder.canManageStores());
        assertTrue(founder.canManageUsers());
        assertTrue(founder.canManageWarehouses());
        assertTrue(founder.canManagePricing());
        assertEquals("Company Founder", founder.getDisplayName());
    }
    
    @Test
    void testGeneralManagerRolePermissions() {
        CompanyRole generalManager = CompanyRole.GENERAL_MANAGER;
        
        assertFalse(generalManager.isOwnerLevel());
        assertTrue(generalManager.isManagerLevel());
        assertTrue(generalManager.canManageStores());
        assertTrue(generalManager.canManageUsers());
        assertTrue(generalManager.canManageWarehouses());
        assertTrue(generalManager.canManagePricing());
        assertEquals("General Manager", generalManager.getDisplayName());
    }
    
    @Test
    void testStoreManagerRolePermissions() {
        CompanyRole storeManager = CompanyRole.STORE_MANAGER;
        
        assertFalse(storeManager.isOwnerLevel());
        assertTrue(storeManager.isManagerLevel());
        assertFalse(storeManager.canManageStores());
        assertFalse(storeManager.canManageUsers());
        assertFalse(storeManager.canManageWarehouses());
        assertFalse(storeManager.canManagePricing());
        assertEquals("Store Manager", storeManager.getDisplayName());
    }
    
    @Test
    void testEmployeeRolePermissions() {
        CompanyRole employee = CompanyRole.EMPLOYEE;
        
        assertFalse(employee.isOwnerLevel());
        assertFalse(employee.isManagerLevel());
        assertFalse(employee.canManageStores());
        assertFalse(employee.canManageUsers());
        assertFalse(employee.canManageWarehouses());
        assertFalse(employee.canManagePricing());
        assertEquals("Employee", employee.getDisplayName());
    }
    
    @Test
    void testRoleHierarchy() {
        // Test that CEO is the highest privilege role
        assertTrue(CompanyRole.CEO.isOwnerLevel());
        assertTrue(CompanyRole.FOUNDER.isOwnerLevel());
        
        // Test manager levels
        assertTrue(CompanyRole.CEO.isManagerLevel());
        assertTrue(CompanyRole.FOUNDER.isManagerLevel());
        assertTrue(CompanyRole.GENERAL_MANAGER.isManagerLevel());
        assertTrue(CompanyRole.STORE_MANAGER.isManagerLevel());
        assertFalse(CompanyRole.EMPLOYEE.isManagerLevel());
    }
}