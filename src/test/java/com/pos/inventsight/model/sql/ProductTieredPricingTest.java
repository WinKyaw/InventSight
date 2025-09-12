package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTieredPricingTest {
    
    private Product product;
    private Store store;
    
    @BeforeEach
    void setUp() {
        store = new Store();
        store.setId(1L);
        store.setStoreName("Test Store");
        
        product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setStore(store);
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("8.00"));
        product.setQuantity(100);
    }
    
    @Test
    void testTieredPricingSetup() {
        assertEquals(new BigDecimal("10.00"), product.getOriginalPrice());
        assertEquals(new BigDecimal("15.00"), product.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("20.00"), product.getRetailPrice());
        assertEquals(store, product.getStore());
    }
    
    @Test
    void testGetPriceForRole() {
        assertEquals(new BigDecimal("10.00"), product.getPriceForRole(UserRole.OWNER));
        assertEquals(new BigDecimal("10.00"), product.getPriceForRole(UserRole.CO_OWNER));
        assertEquals(new BigDecimal("15.00"), product.getPriceForRole(UserRole.MANAGER));
        assertEquals(new BigDecimal("20.00"), product.getPriceForRole(UserRole.EMPLOYEE));
        assertEquals(new BigDecimal("20.00"), product.getPriceForRole(UserRole.CUSTOMER));
    }
    
    @Test
    void testCanViewOriginalPrice() {
        assertTrue(product.canViewOriginalPrice(UserRole.OWNER));
        assertTrue(product.canViewOriginalPrice(UserRole.CO_OWNER));
        assertFalse(product.canViewOriginalPrice(UserRole.MANAGER));
        assertFalse(product.canViewOriginalPrice(UserRole.EMPLOYEE));
        assertFalse(product.canViewOriginalPrice(UserRole.CUSTOMER));
    }
    
    @Test
    void testGetOwnerProfit() {
        BigDecimal ownerProfit = product.getOwnerProfit();
        assertEquals(new BigDecimal("2.00"), ownerProfit); // 10.00 - 8.00
    }
    
    @Test
    void testGetRetailProfit() {
        BigDecimal retailProfit = product.getRetailProfit();
        assertEquals(new BigDecimal("12.00"), retailProfit); // 20.00 - 8.00
    }
    
    @Test
    void testGetOwnerSetProfitMargin() {
        BigDecimal profitMargin = product.getOwnerSetProfitMargin();
        // (15.00 - 8.00) / 8.00 * 100 = 87.5%
        assertEquals(new BigDecimal("87.5000"), profitMargin);
    }
    
    @Test
    void testConstructorWithTieredPricing() {
        Product newProduct = new Product(
            "New Product", 
            "NEW-001", 
            new BigDecimal("12.00"), 
            new BigDecimal("18.00"), 
            new BigDecimal("25.00"), 
            50, 
            store
        );
        
        assertEquals("New Product", newProduct.getName());
        assertEquals("NEW-001", newProduct.getSku());
        assertEquals(new BigDecimal("12.00"), newProduct.getOriginalPrice());
        assertEquals(new BigDecimal("18.00"), newProduct.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("25.00"), newProduct.getRetailPrice());
        assertEquals(new BigDecimal("25.00"), newProduct.getPrice()); // Legacy price set to retail
        assertEquals(Integer.valueOf(50), newProduct.getQuantity());
        assertEquals(store, newProduct.getStore());
    }
    
    @Test
    void testLegacyPriceCompatibility() {
        // Test setting retail price updates legacy price
        product.setRetailPrice(new BigDecimal("22.00"));
        assertEquals(new BigDecimal("22.00"), product.getPrice());
        
        // Test setting legacy price updates retail price if not set
        Product legacyProduct = new Product();
        legacyProduct.setPrice(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("30.00"), legacyProduct.getRetailPrice());
    }
    
    @Test
    void testProfitCalculationsWithNullValues() {
        Product productWithNulls = new Product();
        
        assertEquals(BigDecimal.ZERO, productWithNulls.getOwnerProfit());
        assertEquals(BigDecimal.ZERO, productWithNulls.getRetailProfit());
        assertEquals(BigDecimal.ZERO, productWithNulls.getOwnerSetProfitMargin());
    }
}