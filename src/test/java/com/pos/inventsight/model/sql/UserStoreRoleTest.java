package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreRoleTest {
    
    private User user;
    private Store store;
    private UserStoreRole userStoreRole;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        
        userStoreRole = new UserStoreRole(user, store, UserRole.MANAGER);
    }
    
    @Test
    void testUserStoreRoleCreation() {
        assertNotNull(userStoreRole);
        assertEquals(user, userStoreRole.getUser());
        assertEquals(store, userStoreRole.getStore());
        assertEquals(UserRole.MANAGER, userStoreRole.getRole());
        assertTrue(userStoreRole.getIsActive());
        assertNotNull(userStoreRole.getAssignedAt());
    }
    
    @Test
    void testConstructorWithAssignedBy() {
        UserStoreRole roleWithAssignedBy = new UserStoreRole(user, store, UserRole.OWNER, "admin");
        assertEquals("admin", roleWithAssignedBy.getAssignedBy());
    }
    
    @Test
    void testRevokeRole() {
        String revokedBy = "admin";
        userStoreRole.revokeRole(revokedBy);
        
        assertFalse(userStoreRole.getIsActive());
        assertNotNull(userStoreRole.getRevokedAt());
        assertEquals(revokedBy, userStoreRole.getRevokedBy());
    }
    
    @Test
    void testRestoreRole() {
        // First revoke the role
        userStoreRole.revokeRole("admin");
        
        // Then restore it
        userStoreRole.restoreRole();
        
        assertTrue(userStoreRole.getIsActive());
        assertNull(userStoreRole.getRevokedAt());
        assertNull(userStoreRole.getRevokedBy());
    }
    
    @Test
    void testIsOwnerOrCoOwner() {
        UserStoreRole ownerRole = new UserStoreRole(user, store, UserRole.OWNER);
        UserStoreRole coOwnerRole = new UserStoreRole(user, store, UserRole.CO_OWNER);
        UserStoreRole managerRole = new UserStoreRole(user, store, UserRole.MANAGER);
        
        assertTrue(ownerRole.isOwnerOrCoOwner());
        assertTrue(coOwnerRole.isOwnerOrCoOwner());
        assertFalse(managerRole.isOwnerOrCoOwner());
    }
    
    @Test
    void testIsManager() {
        UserStoreRole managerRole = new UserStoreRole(user, store, UserRole.MANAGER);
        UserStoreRole ownerRole = new UserStoreRole(user, store, UserRole.OWNER);
        
        assertTrue(managerRole.isManager());
        assertFalse(ownerRole.isManager());
    }
    
    @Test
    void testHasAdminAccess() {
        UserStoreRole ownerRole = new UserStoreRole(user, store, UserRole.OWNER);
        UserStoreRole coOwnerRole = new UserStoreRole(user, store, UserRole.CO_OWNER);
        UserStoreRole managerRole = new UserStoreRole(user, store, UserRole.MANAGER);
        UserStoreRole employeeRole = new UserStoreRole(user, store, UserRole.EMPLOYEE);
        
        assertTrue(ownerRole.hasAdminAccess());
        assertTrue(coOwnerRole.hasAdminAccess());
        assertTrue(managerRole.hasAdminAccess());
        assertFalse(employeeRole.hasAdminAccess());
    }
}