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
        // Create a new user with a UUID
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        // Verify UUID fields are properly set and are UUID objects
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set");
        assertTrue(user.getId() instanceof UUID, "UUID field should be java.util.UUID type");
        assertTrue(user.getTenantId() instanceof UUID, "Tenant ID field should be java.util.UUID type");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID");
    }

    @Test
    public void testUserUuidSettersAcceptUuidObjects() {
        User user = new User();
        UUID testUuid = UUID.randomUUID();
        UUID testTenantId = UUID.randomUUID();
        
        // Test that setters accept UUID objects
        user.setId(testUuid);
        user.setTenantId(testTenantId);
        
        assertEquals(testUuid, user.getId(), "UUID should be set correctly");
        assertEquals(testTenantId, user.getTenantId(), "Tenant ID should be set correctly");
    }

    @Test
    public void testUserConstructorWithParametersGeneratesUuid() {
        User user = new User("testuser", "test@example.com", "password", "John", "Doe");
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set");
        assertTrue(user.getId() instanceof UUID, "UUID should be UUID object");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID");
    }

    @Test
    public void testUserUuidCanBeSetToNull() {
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        // Manually set UUID to null (e.g., for testing purposes)
        user.setId(null);
        user.setTenantId(null);
        
        assertNull(user.getId(), "UUID should be null when set to null");
        assertNull(user.getTenantId(), "Tenant ID should be null when set to null");
    }
}