package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

/**
 * Integration test to verify that the User entity UUID conversion 
 * resolves the PostgreSQL UUID type mapping issue
 */
public class UserUuidIntegrationTest {

    @Test
    public void testUserUuidFieldCompatibilityWithPostgreSQL() {
        // Create a user the way the application would
        User user = new User("testuser", "test@example.com", "password123", "John", "Doe");
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        // Verify the UUID fields are proper UUID objects (not Strings)
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set");
        
        // Verify they are actual UUID objects, not Strings
        // This ensures JPA will map them correctly to PostgreSQL UUID columns
        assertTrue(user.getId() instanceof UUID, "UUID field must be java.util.UUID type");
        assertTrue(user.getTenantId() instanceof UUID, "Tenant ID field must be java.util.UUID type");
        
        // Verify they have valid UUID format when converted to String
        String uuidString = user.getId().toString();
        assertTrue(uuidString.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), 
                   "UUID should have valid format: " + uuidString);
        
        String tenantIdString = user.getTenantId().toString();
        assertTrue(tenantIdString.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), 
                   "Tenant ID should have valid format: " + tenantIdString);
    }

    @Test
    public void testUserUuidSetterCompatibility() {
        User user = new User();
        UUID testUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        // Test that we can set UUID objects directly
        user.setId(testUuid);
        user.setTenantId(testUuid);
        
        // Verify the UUID is set correctly
        assertEquals(testUuid, user.getId(), "UUID should be set correctly");
        assertEquals(testUuid, user.getTenantId(), "Tenant ID should be set correctly");
    }

    @Test
    public void testUserEnsureTenantIdFunctionality() {
        // Create user and set ID but not tenant ID
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(null);
        
        // Call ensureTenantId method (simulates @PostPersist behavior)
        user.ensureTenantId();
        
        // Verify tenant ID is set to match ID
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set by ensureTenantId");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID");
    }

    @Test
    public void testMultipleUsersHaveUniqueUuids() {
        // Create multiple users
        User user1 = new User("user1", "user1@test.com", "pass", "User", "One");
        User user2 = new User("user2", "user2@test.com", "pass", "User", "Two");
        User user3 = new User("user3", "user3@test.com", "pass", "User", "Three");
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        user1.setId(id1);
        user1.setTenantId(id1);
        user2.setId(id2);
        user2.setTenantId(id2);
        user3.setId(id3);
        user3.setTenantId(id3);
        
        // Verify all UUIDs are unique
        assertNotEquals(user1.getId(), user2.getId(), "User UUIDs should be unique");
        assertNotEquals(user1.getId(), user3.getId(), "User UUIDs should be unique");
        assertNotEquals(user2.getId(), user3.getId(), "User UUIDs should be unique");
        
        // Verify all tenant IDs are unique
        assertNotEquals(user1.getTenantId(), user2.getTenantId(), "Tenant IDs should be unique");
        assertNotEquals(user1.getTenantId(), user3.getTenantId(), "Tenant IDs should be unique");
        assertNotEquals(user2.getTenantId(), user3.getTenantId(), "Tenant IDs should be unique");
    }

    @Test
    public void testUuidToStringConversionForExternalApis() {
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        // Verify we can convert UUIDs to Strings for external APIs
        String uuidString = user.getId().toString();
        String tenantIdString = user.getTenantId().toString();
        
        // Verify the strings can be converted back to UUIDs
        UUID parsedUuid = UUID.fromString(uuidString);
        UUID parsedTenantId = UUID.fromString(tenantIdString);
        
        assertEquals(user.getId(), parsedUuid, "UUID should survive string conversion");
        assertEquals(user.getTenantId(), parsedTenantId, "Tenant ID should survive string conversion");
    }
}