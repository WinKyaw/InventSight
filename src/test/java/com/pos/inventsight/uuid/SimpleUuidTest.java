package com.pos.inventsight.uuid;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test to verify UUID functionality without Spring context
 */
public class SimpleUuidTest {

    @Test
    public void testStoreUuidGeneration() {
        Store store = new Store();
        store.setStoreName("Test Store");
        store.setAddress("123 Test St");
        store.setCity("Test City");
        store.setState("TS");
        store.setCountry("Test Country");
        
        // Verify UUID is generated in constructor
        assertNotNull(store.getId());
        assertTrue(store.getId() instanceof UUID);
        
        // Verify UUID format is valid
        String uuidString = store.getId().toString();
        assertTrue(uuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    public void testProductUuidGeneration() {
        // Create store first
        Store store = new Store("Test Store", "123 Test St", "Test City", "TS", "Test Country");
        
        // Create product
        Product product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setOriginalPrice(BigDecimal.valueOf(10.00));
        product.setOwnerSetSellPrice(BigDecimal.valueOf(12.00));
        product.setRetailPrice(BigDecimal.valueOf(15.00));
        product.setQuantity(100);
        product.setStore(store);
        
        // Verify UUID is generated in constructor
        assertNotNull(product.getId());
        assertTrue(product.getId() instanceof UUID);
        
        // Verify UUID format is valid
        String uuidString = product.getId().toString();
        assertTrue(uuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
        
        // Verify store relationship
        assertEquals(store, product.getStore());
        assertEquals(store.getId(), product.getStore().getId());
    }

    @Test
    public void testProductConstructorWithStore() {
        Store store = new Store("Constructor Store", "456 Test Ave", "Test City", "TS", "Test Country");
        
        Product product = new Product("Constructor Product", "CONS-001", 
                BigDecimal.valueOf(20.00), BigDecimal.valueOf(25.00), 
                BigDecimal.valueOf(30.00), 50, store);
        
        // Verify both have valid UUIDs
        assertNotNull(store.getId());
        assertNotNull(product.getId());
        assertTrue(store.getId() instanceof UUID);
        assertTrue(product.getId() instanceof UUID);
        
        // Verify they are different UUIDs
        assertNotEquals(store.getId(), product.getId());
        
        // Verify product properties
        assertEquals("Constructor Product", product.getName());
        assertEquals("CONS-001", product.getSku());
        assertEquals(BigDecimal.valueOf(20.00), product.getOriginalPrice());
        assertEquals(BigDecimal.valueOf(25.00), product.getOwnerSetSellPrice());
        assertEquals(BigDecimal.valueOf(30.00), product.getRetailPrice());
        assertEquals(Integer.valueOf(50), product.getQuantity());
        assertEquals(store, product.getStore());
    }

    @Test
    public void testUuidUniqueness() {
        // Create multiple stores and products to verify UUID uniqueness
        Store store1 = new Store();
        Store store2 = new Store();
        Product product1 = new Product();
        Product product2 = new Product();
        
        // All UUIDs should be different
        assertNotEquals(store1.getId(), store2.getId());
        assertNotEquals(product1.getId(), product2.getId());
        assertNotEquals(store1.getId(), product1.getId());
        assertNotEquals(store2.getId(), product2.getId());
    }
}