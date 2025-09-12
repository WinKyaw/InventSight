package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

/**
 * Test for User UUID field type conversion
 * Validates that User entity properly handles UUID objects instead of Strings
 */
public class UserUuidTest {

    @Test
    public void testUserUuidFieldsAreUuidType() {
        // Create a new user
        User user = new User();
        
        // Verify UUID fields are properly set and are UUID objects
        assertNotNull(user.getUuid(), "UUID should be automatically generated");
        assertNotNull(user.getTenantId(), "Tenant ID should be automatically set");
        assertTrue(user.getUuid() instanceof UUID, "UUID field should be java.util.UUID type");
        assertTrue(user.getTenantId() instanceof UUID, "Tenant ID field should be java.util.UUID type");
        assertEquals(user.getUuid(), user.getTenantId(), "Tenant ID should equal UUID by default");
    }

    @Test
    public void testUserUuidSettersAcceptUuidObjects() {
        User user = new User();
        UUID testUuid = UUID.randomUUID();
        UUID testTenantId = UUID.randomUUID();
        
        // Test that setters accept UUID objects
        user.setUuid(testUuid);
        user.setTenantId(testTenantId);
        
        assertEquals(testUuid, user.getUuid(), "UUID should be set correctly");
        assertEquals(testTenantId, user.getTenantId(), "Tenant ID should be set correctly");
    }

    @Test
    public void testUserConstructorWithParametersGeneratesUuid() {
        User user = new User("testuser", "test@example.com", "password", "John", "Doe");
        
        assertNotNull(user.getUuid(), "Constructor should generate UUID");
        assertNotNull(user.getTenantId(), "Constructor should set tenant ID");
        assertTrue(user.getUuid() instanceof UUID, "Generated UUID should be UUID object");
        assertEquals(user.getUuid(), user.getTenantId(), "Tenant ID should equal UUID by default");
    }

    @Test
    public void testEnsureUuidMethodHandlesNullValues() {
        User user = new User();
        // Manually set UUID to null to test the ensureUuid method
        user.setUuid(null);
        user.setTenantId(null);
        
        // Call ensureUuid (normally called by @PrePersist/@PreUpdate)
        user.ensureUuid();
        
        assertNotNull(user.getUuid(), "ensureUuid should generate UUID when null");
        assertNotNull(user.getTenantId(), "ensureUuid should set tenant ID when null");
        assertEquals(user.getUuid(), user.getTenantId(), "Tenant ID should equal UUID after ensureUuid");
    }
}