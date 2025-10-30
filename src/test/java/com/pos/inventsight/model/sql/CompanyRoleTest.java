package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyRoleTest {
    
    @Test
    void testFounderRolePermissions() {
        CompanyRole founder = CompanyRole.FOUNDER;
        
        assertTrue(founder.isOwnerLevel());
        assertTrue(founder.isManagerLevel());
        assertTrue(founder.canManageStores());
        assertTrue(founder.canManageUsers());
        assertTrue(founder.canManageWarehouses());
        assertEquals("Company Founder", founder.getDisplayName());
    }
    
    @Test
    void testCeoRolePermissions() {
        CompanyRole ceo = CompanyRole.CEO;
        
        assertTrue(ceo.isOwnerLevel());
        assertTrue(ceo.isManagerLevel());
        assertTrue(ceo.canManageStores());
        assertTrue(ceo.canManageUsers());
        assertTrue(ceo.canManageWarehouses());
        assertEquals("Chief Executive Officer", ceo.getDisplayName());
    }
    
    @Test
    void testGeneralManagerRolePermissions() {
        CompanyRole generalManager = CompanyRole.GENERAL_MANAGER;
        
        assertFalse(generalManager.isOwnerLevel());
        assertTrue(generalManager.isManagerLevel());
        assertTrue(generalManager.canManageStores());
        assertTrue(generalManager.canManageUsers());
        assertTrue(generalManager.canManageWarehouses());
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
        assertEquals("Employee", employee.getDisplayName());
    }
}