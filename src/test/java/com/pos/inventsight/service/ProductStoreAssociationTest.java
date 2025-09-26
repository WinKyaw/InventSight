package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
@DisplayName("Product Store Association Tests")
class ProductStoreAssociationTest {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        // Create a test user (this will auto-create a store)
        testUser = new User();
        testUser.setUsername("prodtestuser_" + System.currentTimeMillis());
        testUser.setEmail("prodtestuser_" + System.currentTimeMillis() + "@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Product");
        testUser.setLastName("Tester");
        
        testUser = userService.createUser(testUser);
        
        // Get the auto-created store
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(testUser);
        if (!userStoreRoles.isEmpty()) {
            testStore = userStoreRoles.get(0).getStore();
        }
        
        // Set tenant context to the test user's UUID
        TenantContext.setCurrentTenant(testUser.getUuid().toString());
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Creating a product should automatically associate it with current user's store")
    void testProductCreationAssociatesWithUserStore() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("A test product");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(100);
        product.setCategory("Test Category");
        
        // Act
        Product savedProduct = productService.createProduct(product, testUser.getUsername());
        
        // Assert
        assertNotNull(savedProduct);
        assertNotNull(savedProduct.getId());
        assertNotNull(savedProduct.getStore(), "Product should be associated with a store");
        
        Store associatedStore = savedProduct.getStore();
        assertNotNull(associatedStore.getId());
        assertEquals("My Store", associatedStore.getStoreName());
        assertEquals(testUser.getUsername(), associatedStore.getCreatedBy());
        
        System.out.println("✅ Test passed: Product " + savedProduct.getName() + 
                         " associated with store " + associatedStore.getStoreName() +
                         " (Store ID: " + associatedStore.getId() + ")");
    }
    
    @Test
    @DisplayName("Product creation should fail when no store is available and none is explicitly set")
    void testProductCreationFailsWithoutStore() {
        // Clear tenant context to simulate no current user context
        TenantContext.clear();
        
        // Arrange
        Product product = new Product();
        product.setName("Test Product No Store");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(50);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            productService.createProduct(product, "testuser");
        });
        
        assertTrue(exception.getMessage().contains("No active store found"), 
                  "Should throw exception about no active store");
        
        System.out.println("✅ Test passed: Product creation correctly failed with message: " + 
                         exception.getMessage());
    }
}