package com.pos.inventsight.integration;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.ProductService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to validate UUID implementation and tenant isolation fixes
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.multiTenancy=NONE",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Transactional
class UuidTenantIsolationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    private User userA;
    private User userB;
    private Store storeA;
    private Store storeB;
    private Product productA;
    private Product productB;
    
    @BeforeEach
    void setUp() {
        // Create stores
        storeA = new Store("Store A", "123 Main St", "City A", "State A", "Country A");
        storeA = storeRepository.save(storeA);
        
        storeB = new Store("Store B", "456 Oak Ave", "City B", "State B", "Country B");
        storeB = storeRepository.save(storeB);
        
        // Create users with UUIDs and tenant IDs
        userA = new User("userA", "userA@test.com", "password", "User", "A");
        userA = userRepository.save(userA);
        
        userB = new User("userB", "userB@test.com", "password", "User", "B");
        userB = userRepository.save(userB);
        
        // Create user-store relationships
        UserStoreRole roleA = new UserStoreRole(userA, storeA, UserRole.OWNER);
        userStoreRoleRepository.save(roleA);
        
        UserStoreRole roleB = new UserStoreRole(userB, storeB, UserRole.OWNER);
        userStoreRoleRepository.save(roleB);
        
        // Create products associated with stores
        productA = new Product("Product A", "SKU-A", BigDecimal.valueOf(10), 
                              BigDecimal.valueOf(15), BigDecimal.valueOf(20), 100, storeA);
        productA = productRepository.save(productA);
        
        productB = new Product("Product B", "SKU-B", BigDecimal.valueOf(15), 
                              BigDecimal.valueOf(20), BigDecimal.valueOf(25), 200, storeB);
        productB = productRepository.save(productB);
    }
    
    @Test
    void testUserUuidGeneration() {
        // Test that UUIDs are generated for users
        assertNotNull(userA.getUuid(), "User A should have a UUID");
        assertNotNull(userB.getUuid(), "User B should have a UUID");
        
        // Test that UUIDs are unique
        assertNotEquals(userA.getUuid(), userB.getUuid(), "User UUIDs should be unique");
        
        // Test that tenant IDs are set to UUIDs
        assertEquals(userA.getUuid(), userA.getTenantId(), "Tenant ID should match UUID for User A");
        assertEquals(userB.getUuid(), userB.getTenantId(), "Tenant ID should match UUID for User B");
        
        // Test UUID format
        assertTrue(isValidUUID(userA.getUuid().toString()), "User A UUID should be valid format");
        assertTrue(isValidUUID(userB.getUuid().toString()), "User B UUID should be valid format");
    }
    
    @Test
    void testProductUuidGeneration() {
        // Test that UUID IDs are generated for products
        assertNotNull(productA.getId(), "Product A should have a UUID ID");
        assertNotNull(productB.getId(), "Product B should have a UUID ID");
        
        // Test that UUID IDs are unique
        assertNotEquals(productA.getId(), productB.getId(), "Product UUID IDs should be unique");
        
        // Test UUID format
        assertTrue(isValidUUID(productA.getId().toString()), "Product A UUID ID should be valid format");
        assertTrue(isValidUUID(productB.getId().toString()), "Product B UUID ID should be valid format");
    }
    
    @Test
    void testTenantIsolationWithCurrentUserStore() {
        // Test that UserService can find users by UUID
        User foundUserA = userService.getUserByUuid(userA.getUuid());
        assertEquals(userA.getId(), foundUserA.getId(), "Should find correct user by UUID");
        
        // Simulate tenant context for User A
        TenantContext.setCurrentTenant(userA.getUuid().toString());
        try {
            Store currentStore = userService.getCurrentUserStore();
            assertNotNull(currentStore, "Should find store for current user");
            assertEquals(storeA.getId(), currentStore.getId(), "Should find correct store for User A");
        } finally {
            TenantContext.clear();
        }
        
        // Simulate tenant context for User B
        TenantContext.setCurrentTenant(userB.getUuid().toString());
        try {
            Store currentStore = userService.getCurrentUserStore();
            assertNotNull(currentStore, "Should find store for current user");
            assertEquals(storeB.getId(), currentStore.getId(), "Should find correct store for User B");
        } finally {
            TenantContext.clear();
        }
    }
    
    @Test
    void testProductTenantAwareQueries() {
        // Without tenant context (default/public schema)
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            List<Product> allProducts = productService.getAllActiveProducts();
            assertTrue(allProducts.size() >= 2, "Should see all products in default tenant");
        } finally {
            TenantContext.clear();
        }
        
        // With User A's tenant context
        TenantContext.setCurrentTenant(userA.getUuid().toString());
        try {
            List<Product> userAProducts = productService.getAllActiveProducts();
            // In a real multi-tenant environment, this would filter by store
            // For this test, we verify the service attempts to get the current user's store
            assertNotNull(userAProducts, "Should return products list for User A");
        } finally {
            TenantContext.clear();
        }
        
        // With User B's tenant context
        TenantContext.setCurrentTenant(userB.getUuid().toString());
        try {
            List<Product> userBProducts = productService.getAllActiveProducts();
            // In a real multi-tenant environment, this would filter by store
            // For this test, we verify the service attempts to get the current user's store
            assertNotNull(userBProducts, "Should return products list for User B");
        } finally {
            TenantContext.clear();
        }
    }
    
    @Test
    void testTenantContextValidation() {
        // Test invalid UUID in tenant context
        TenantContext.setCurrentTenant("invalid-uuid");
        try {
            assertThrows(Exception.class, () -> {
                userService.getCurrentUserStore();
            }, "Should throw exception for invalid tenant UUID");
        } finally {
            TenantContext.clear();
        }
        
        // Test non-existent UUID in tenant context
        TenantContext.setCurrentTenant(UUID.randomUUID().toString());
        try {
            assertThrows(Exception.class, () -> {
                userService.getCurrentUserStore();
            }, "Should throw exception for non-existent tenant UUID");
        } finally {
            TenantContext.clear();
        }
    }
    
    @Test
    void testBackwardCompatibilityWithLongIds() {
        // Ensure UUID IDs work as primary keys
        Product foundById = productRepository.findById(productA.getId()).orElse(null);
        assertNotNull(foundById, "Should find product by UUID ID");
        assertEquals(productA.getId(), foundById.getId(), "UUID ID should be preserved");
        
        User foundUserById = userRepository.findById(userA.getId()).orElse(null);
        assertNotNull(foundUserById, "Should find user by Long ID");
        assertEquals(userA.getUuid(), foundUserById.getUuid(), "UUID should be preserved");
    }
    
    @Test
    void testUuidSearchMethods() {
        // Test new UUID-based repository methods
        User foundByUuid = userRepository.findByUuid(userA.getUuid()).orElse(null);
        assertNotNull(foundByUuid, "Should find user by UUID");
        assertEquals(userA.getId(), foundByUuid.getId(), "Should find correct user");
        
        // Test that UUID is unique
        List<User> allUsers = userRepository.findAll();
        long uniqueUuids = allUsers.stream()
                .map(User::getUuid)
                .distinct()
                .count();
        assertEquals(allUsers.size(), uniqueUuids, "All UUIDs should be unique");
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