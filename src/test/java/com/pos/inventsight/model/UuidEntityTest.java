package com.pos.inventsight.model;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for UUID functionality in User and Product entities
 */
class UuidEntityTest {
    
    @Test
    void testUserUuidGeneration() {
        // Test UUID can be set and retrieved
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID");
        
        // Test UUID format is valid
        assertTrue(isValidUUID(user.getId().toString()), "UUID should be valid format");
    }
    
    @Test
    void testUserConstructorWithParameters() {
        User user = new User("testuser", "test@example.com", "password", "John", "Doe");
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID");
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
    }
    
    @Test
    void testUserUuidUniqueness() {
        User user1 = new User();
        User user2 = new User();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        user1.setId(id1);
        user1.setTenantId(id1);
        user2.setId(id2);
        user2.setTenantId(id2);
        
        assertNotEquals(user1.getId(), user2.getId(), "UUIDs should be unique");
        assertNotEquals(user1.getTenantId(), user2.getTenantId(), "Tenant IDs should be unique");
    }
    
    @Test
    void testUserUuidSetter() {
        User user = new User();
        UUID oldUuid = UUID.randomUUID();
        UUID newUuid = UUID.randomUUID();
        user.setId(oldUuid);
        user.setTenantId(oldUuid);
        
        user.setId(newUuid);
        user.setTenantId(newUuid);
        
        assertEquals(newUuid, user.getId(), "UUID should be updated");
        assertEquals(newUuid, user.getTenantId(), "Tenant ID should be updated");
        assertNotEquals(oldUuid, user.getId(), "UUID should be different from original");
    }
    
    @Test
    void testProductUuidGeneration() {
        // Test UUID is generated automatically in constructor
        Product product = new Product();
        
        assertNotNull(product.getId(), "UUID ID should be generated automatically");
        assertTrue(isValidUUID(product.getId().toString()), "UUID ID should be valid format");
    }
    
    @Test
    void testProductConstructorWithParameters() {
        Store store = new Store("Test Store", "Address", "City", "State", "Country");
        Product product = new Product("Test Product", "SKU-001", 
                                    BigDecimal.valueOf(10), BigDecimal.valueOf(15), 
                                    BigDecimal.valueOf(20), 100, store);
        
        assertNotNull(product.getId(), "UUID ID should be generated in parameterized constructor");
        assertEquals("Test Product", product.getName());
        assertEquals("SKU-001", product.getSku());
        assertEquals(store, product.getStore());
    }
    
    @Test
    void testProductUuidUniqueness() {
        Product product1 = new Product();
        Product product2 = new Product();
        
        assertNotEquals(product1.getId(), product2.getId(), "Product UUID IDs should be unique");
    }
    
    @Test
    void testProductUuidPersistence() {
        Product product = new Product();
        UUID originalId = product.getId();
        
        // Simulate saving - the ID should remain the same
        assertNotNull(originalId, "Product UUID ID should be set");
        assertEquals(originalId, product.getId(), "Product UUID ID should remain consistent");
    }
    
    @Test
    void testUserPostPersistHook() {
        User user = new User();
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(null); // Clear tenant ID
        
        // Simulate JPA PostPersist call
        user.ensureTenantId();
        
        assertNotNull(user.getId(), "UUID should be set");
        assertNotNull(user.getTenantId(), "Tenant ID should be set by PostPersist");
        assertEquals(user.getId(), user.getTenantId(), "Tenant ID should equal UUID after PostPersist");
    }
    
    @Test
    void testProductIdGeneration() {
        Product product = new Product();
        
        // UUID should be auto-generated by Hibernate
        assertNotNull(product.getId(), "UUID ID should be automatically generated");
        assertTrue(isValidUUID(product.getId().toString()), "Generated ID should be valid UUID");
    }
    
    @Test
    void testUserPostPersistDoesNotOverwriteExistingTenantId() {
        User user = new User();
        UUID testId = UUID.randomUUID();
        UUID testTenantId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testTenantId);
        
        // Call ensureTenantId when tenantId already exists
        user.ensureTenantId();
        
        assertEquals(testTenantId, user.getTenantId(), "Existing tenant ID should not be overwritten");
    }
    
    @Test
    void testUserEntityIntegrity() {
        User user = new User("user123", "user@test.com", "password", "Test", "User");
        UUID testId = UUID.randomUUID();
        user.setId(testId);
        user.setTenantId(testId);
        user.setRole(UserRole.OWNER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        
        // Validate all required fields are preserved along with UUID
        assertEquals("user123", user.getUsername());
        assertEquals("user@test.com", user.getEmail());
        assertEquals(UserRole.OWNER, user.getRole());
        assertTrue(user.getIsActive());
        assertTrue(user.getEmailVerified());
        assertNotNull(user.getId());
        assertNotNull(user.getTenantId());
        
        // Test UserDetails methods still work
        assertTrue(user.isEnabled());
        assertEquals("ROLE_OWNER", user.getAuthorities().iterator().next().getAuthority());
    }
    
    // Helper method to validate UUID format
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}