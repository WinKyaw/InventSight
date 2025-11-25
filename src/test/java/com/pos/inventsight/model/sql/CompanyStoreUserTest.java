package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyStoreUserTest {
    
    private Company testCompany;
    private Store testStore;
    private User testUser;
    private CompanyStoreUser companyStoreUser;
    
    @BeforeEach
    void setUp() {
        testCompany = new Company("Test Company", "test@company.com");
        testCompany.setId(UUID.randomUUID());
        
        testStore = new Store("Test Store", "123 Main St", "City", "State", "Country");
        testStore.setId(UUID.randomUUID());
        
        testUser = new User("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(UUID.randomUUID());
        testUser.setUuid(UUID.randomUUID());
        
        companyStoreUser = new CompanyStoreUser(testCompany, testStore, testUser, CompanyRole.STORE_MANAGER, "admin");
    }
    
    @Test
    void testConstructorWithStore() {
        // Note: ID will be null since we're not using JPA context
        assertEquals(testCompany, companyStoreUser.getCompany());
        assertEquals(testStore, companyStoreUser.getStore());
        assertEquals(testUser, companyStoreUser.getUser());
        assertEquals(CompanyRole.STORE_MANAGER, companyStoreUser.getRole());
        assertEquals("admin", companyStoreUser.getAssignedBy());
        assertTrue(companyStoreUser.getIsActive());
        assertNotNull(companyStoreUser.getAssignedAt());
    }
    
    @Test
    void testConstructorWithoutStore() {
        CompanyStoreUser companyLevel = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER, "system");
        
        // Note: ID will be null since we're not using JPA context
        assertEquals(testCompany, companyLevel.getCompany());
        assertNull(companyLevel.getStore());
        assertEquals(testUser, companyLevel.getUser());
        assertEquals(CompanyRole.FOUNDER, companyLevel.getRole());
        assertEquals("system", companyLevel.getAssignedBy());
        assertTrue(companyLevel.getIsActive());
    }
    
    @Test
    void testIsCompanyLevel() {
        CompanyStoreUser companyLevel = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER);
        
        assertTrue(companyLevel.isCompanyLevel());
        assertFalse(companyLevel.isStoreSpecific());
    }
    
    @Test
    void testIsStoreSpecific() {
        assertTrue(companyStoreUser.isStoreSpecific());
        assertFalse(companyStoreUser.isCompanyLevel());
    }
    
    @Test
    void testRolePermissions() {
        // Test store manager permissions
        assertFalse(companyStoreUser.isOwnerLevel());
        assertTrue(companyStoreUser.isManagerLevel());
        assertFalse(companyStoreUser.canManageStores()); // Store-specific, not company-level
        assertFalse(companyStoreUser.canManageUsers());
        assertFalse(companyStoreUser.canManageWarehouses());
        
        // Test founder permissions
        CompanyStoreUser founder = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER);
        assertTrue(founder.isOwnerLevel());
        assertTrue(founder.isManagerLevel());
        assertTrue(founder.canManageStores()); // Company-level founder
        assertTrue(founder.canManageUsers());
        assertTrue(founder.canManageWarehouses());
    }
    
    @Test
    void testRevokeRole() {
        String revokedBy = "manager";
        LocalDateTime beforeRevoke = LocalDateTime.now();
        
        companyStoreUser.revokeRole(revokedBy);
        
        assertFalse(companyStoreUser.getIsActive());
        assertEquals(revokedBy, companyStoreUser.getRevokedBy());
        assertNotNull(companyStoreUser.getRevokedAt());
        assertTrue(companyStoreUser.getRevokedAt().isAfter(beforeRevoke));
        assertNotNull(companyStoreUser.getUpdatedAt());
    }
    
    @Test
    void testRestoreRole() {
        // First revoke the role
        companyStoreUser.revokeRole("manager");
        assertFalse(companyStoreUser.getIsActive());
        
        // Then restore it
        LocalDateTime beforeRestore = LocalDateTime.now();
        companyStoreUser.restoreRole();
        
        assertTrue(companyStoreUser.getIsActive());
        assertNull(companyStoreUser.getRevokedBy());
        assertNull(companyStoreUser.getRevokedAt());
        assertNotNull(companyStoreUser.getUpdatedAt());
        assertTrue(companyStoreUser.getUpdatedAt().isAfter(beforeRestore));
    }
}