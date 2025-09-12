package com.pos.inventsight.integration;

import com.pos.inventsight.model.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to validate multi-tenancy, RBAC, and tiered pricing work together
 */
class MultiTenantIntegrationTest {
    
    private Store store1;
    private Store store2;
    private User owner;
    private User manager;
    private User employee;
    private Product product;
    
    @BeforeEach
    void setUp() {
        // Create stores
        store1 = new Store("Coffee Shop Alpha", "123 Main St", "Downtown", "NY", "USA");
        store1.setId(1L);
        
        store2 = new Store("Coffee Shop Beta", "456 Oak Ave", "Uptown", "NY", "USA");
        store2.setId(2L);
        
        // Create users
        owner = new User("owner", "owner@alpha.com", "password", "John", "Owner");
        owner.setId(1L);
        
        manager = new User("manager", "manager@alpha.com", "password", "Jane", "Manager");
        manager.setId(2L);
        
        employee = new User("employee", "employee@alpha.com", "password", "Bob", "Employee");
        employee.setId(3L);
        
        // Create product with tiered pricing
        product = new Product();
        product.setId(1L);
        product.setName("Premium Coffee");
        product.setSku("COFFEE-001");
        product.setStore(store1);
        product.setCostPrice(new BigDecimal("5.00"));
        product.setOriginalPrice(new BigDecimal("8.00"));
        product.setOwnerSetSellPrice(new BigDecimal("12.00"));
        product.setRetailPrice(new BigDecimal("15.00"));
        product.setQuantity(100);
    }
    
    @Test
    void testMultiTenantDataIsolation() {
        // Products should be isolated by store
        assertEquals(store1, product.getStore());
        assertNotEquals(store2, product.getStore());
        
        // Store information should be properly set
        assertNotNull(store1.getStoreName());
        assertNotNull(store2.getStoreName());
        assertNotEquals(store1.getStoreName(), store2.getStoreName());
    }
    
    @Test
    void testRoleBasedAccessControl() {
        // Create role mappings
        UserStoreRole ownerRole = new UserStoreRole(owner, store1, UserRole.OWNER);
        UserStoreRole managerRole = new UserStoreRole(manager, store1, UserRole.MANAGER);
        UserStoreRole employeeRole = new UserStoreRole(employee, store1, UserRole.EMPLOYEE);
        
        // Test role permissions
        assertTrue(ownerRole.hasAdminAccess());
        assertTrue(managerRole.hasAdminAccess());
        assertFalse(employeeRole.hasAdminAccess());
        
        assertTrue(ownerRole.isOwnerOrCoOwner());
        assertFalse(managerRole.isOwnerOrCoOwner());
        assertFalse(employeeRole.isOwnerOrCoOwner());
    }
    
    @Test
    void testTieredPricingByRole() {
        // Owner should see original cost price
        assertEquals(new BigDecimal("8.00"), product.getPriceForRole(UserRole.OWNER));
        assertEquals(new BigDecimal("8.00"), product.getPriceForRole(UserRole.CO_OWNER));
        
        // Manager should see owner set sell price
        assertEquals(new BigDecimal("12.00"), product.getPriceForRole(UserRole.MANAGER));
        
        // Employee and customer should see retail price
        assertEquals(new BigDecimal("15.00"), product.getPriceForRole(UserRole.EMPLOYEE));
        assertEquals(new BigDecimal("15.00"), product.getPriceForRole(UserRole.CUSTOMER));
    }
    
    @Test
    void testOriginalPriceVisibility() {
        // Only owners and co-owners can view original prices
        assertTrue(product.canViewOriginalPrice(UserRole.OWNER));
        assertTrue(product.canViewOriginalPrice(UserRole.CO_OWNER));
        assertFalse(product.canViewOriginalPrice(UserRole.MANAGER));
        assertFalse(product.canViewOriginalPrice(UserRole.EMPLOYEE));
        assertFalse(product.canViewOriginalPrice(UserRole.CUSTOMER));
    }
    
    @Test
    void testProfitCalculations() {
        // Test profit margin calculations for different roles
        BigDecimal ownerProfit = product.getOwnerProfit();
        BigDecimal retailProfit = product.getRetailProfit();
        
        assertEquals(new BigDecimal("3.00"), ownerProfit); // 8.00 - 5.00
        assertEquals(new BigDecimal("10.00"), retailProfit); // 15.00 - 5.00
        
        // Owner set profit margin: (12.00 - 5.00) / 5.00 * 100 = 140%
        assertEquals(new BigDecimal("140.0000"), product.getOwnerSetProfitMargin());
    }
    
    @Test
    void testDiscountAuditLogCreation() {
        // Create a discount audit log
        DiscountAuditLog auditLog = new DiscountAuditLog(
            employee,
            UserRole.EMPLOYEE,
            store1,
            product,
            new BigDecimal("12.00"), // attempted price
            new BigDecimal("15.00"), // retail price
            DiscountResult.PENDING_APPROVAL
        );
        
        assertEquals(employee, auditLog.getUser());
        assertEquals(store1, auditLog.getStore());
        assertEquals(product, auditLog.getProduct());
        assertEquals(new BigDecimal("3.00"), auditLog.getDiscountAmount());
        assertEquals(new BigDecimal("20.0000"), auditLog.getDiscountPercentage());
        assertFalse(auditLog.isApproved());
    }
    
    @Test
    void testUserStoreRoleManagement() {
        UserStoreRole userRole = new UserStoreRole(manager, store1, UserRole.MANAGER, "owner");
        
        // Test initial state
        assertTrue(userRole.getIsActive());
        assertEquals(UserRole.MANAGER, userRole.getRole());
        assertEquals("owner", userRole.getAssignedBy());
        
        // Test role revocation
        userRole.revokeRole("admin");
        assertFalse(userRole.getIsActive());
        assertNotNull(userRole.getRevokedAt());
        assertEquals("admin", userRole.getRevokedBy());
        
        // Test role restoration
        userRole.restoreRole();
        assertTrue(userRole.getIsActive());
        assertNull(userRole.getRevokedAt());
        assertNull(userRole.getRevokedBy());
    }
    
    @Test
    void testBackwardCompatibility() {
        // Test that legacy price field is maintained
        assertNotNull(product.getPrice());
        assertEquals(product.getRetailPrice(), product.getPrice());
        
        // Test setting legacy price updates retail price if not set
        Product legacyProduct = new Product();
        legacyProduct.setPrice(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("20.00"), legacyProduct.getRetailPrice());
    }
    
    @Test
    void testCompleteWorkflow() {
        // Simulate a complete workflow
        
        // 1. Owner creates product with tiered pricing
        assertNotNull(product.getStore());
        assertEquals(store1, product.getStore());
        
        // 2. Employee attempts discount
        DiscountAuditLog discountAttempt = new DiscountAuditLog(
            employee,
            UserRole.EMPLOYEE,
            store1,
            product,
            new BigDecimal("13.00"),
            new BigDecimal("15.00"),
            DiscountResult.PENDING_APPROVAL
        );
        
        // 3. Manager approves discount
        discountAttempt.setResult(DiscountResult.APPROVED);
        discountAttempt.setApprovedBy("manager");
        
        // 4. Verify audit trail
        assertTrue(discountAttempt.isApproved());
        assertEquals("manager", discountAttempt.getApprovedBy());
        assertEquals(new BigDecimal("2.00"), discountAttempt.getDiscountAmount());
        
        // 5. Verify role-based pricing still works
        assertEquals(new BigDecimal("8.00"), product.getPriceForRole(UserRole.OWNER));
        assertEquals(new BigDecimal("15.00"), product.getPriceForRole(UserRole.EMPLOYEE));
    }
}