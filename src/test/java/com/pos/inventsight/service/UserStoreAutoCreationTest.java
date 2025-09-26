package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.model.sql.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Store Auto-Creation Unit Tests")
class UserStoreAutoCreationUnitTest {

    @Test
    @DisplayName("UserStoreRole should correctly link User and Store with Owner role")
    void testUserStoreRoleCreation() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        
        Store store = new Store();
        store.setStoreName("My Store");
        store.setDescription("Default store for Test User");
        
        // Act
        UserStoreRole userStoreRole = new UserStoreRole(user, store, UserRole.OWNER, user.getUsername());
        
        // Assert
        assertNotNull(userStoreRole);
        assertEquals(user, userStoreRole.getUser());
        assertEquals(store, userStoreRole.getStore());
        assertEquals(UserRole.OWNER, userStoreRole.getRole());
        assertEquals(user.getUsername(), userStoreRole.getAssignedBy());
        assertTrue(userStoreRole.getIsActive());
        assertTrue(userStoreRole.isOwnerOrCoOwner());
        assertTrue(userStoreRole.hasAdminAccess());
        
        System.out.println("✅ Test passed: UserStoreRole created successfully");
        System.out.println("   User: " + userStoreRole.getUser().getUsername());
        System.out.println("   Store: " + userStoreRole.getStore().getStoreName());
        System.out.println("   Role: " + userStoreRole.getRole());
    }
    
    @Test
    @DisplayName("Store constructor should set basic fields correctly")
    void testStoreCreation() {
        // Arrange & Act
        Store store = new Store("My Store", "123 Main St", "Test City", "Test State", "Test Country");
        store.setDescription("Test store description");
        
        // Assert
        assertEquals("My Store", store.getStoreName());
        assertEquals("123 Main St", store.getAddress());
        assertEquals("Test City", store.getCity());
        assertEquals("Test State", store.getState());
        assertEquals("Test Country", store.getCountry());
        assertEquals("Test store description", store.getDescription());
        
        System.out.println("✅ Test passed: Store created with constructor");
        System.out.println("   Name: " + store.getStoreName());
        System.out.println("   Address: " + store.getFullAddress());
    }
}