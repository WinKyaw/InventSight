package com.pos.inventsight.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify ProductRequest can accept both legacy and new pricing fields
 * This validates the fix for the JPA transaction rollback issue
 */
@DisplayName("ProductRequest Tiered Pricing Tests")
class ProductRequestPricingTest {
    
    @Test
    @DisplayName("ProductRequest should accept all three tiered pricing fields")
    void testProductRequestWithTieredPricing() {
        // Given: Create a ProductRequest with tiered pricing
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setCategory("Electronics");
        request.setSku("TEST-001");
        request.setQuantity(10);
        
        // Set all three pricing fields
        request.setOriginalPrice(new BigDecimal("10.00"));
        request.setOwnerSetSellPrice(new BigDecimal("15.00"));
        request.setRetailPrice(new BigDecimal("20.00"));
        
        // Then: All pricing fields should be accessible
        assertNotNull(request.getOriginalPrice());
        assertNotNull(request.getOwnerSetSellPrice());
        assertNotNull(request.getRetailPrice());
        
        assertEquals(new BigDecimal("10.00"), request.getOriginalPrice());
        assertEquals(new BigDecimal("15.00"), request.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("20.00"), request.getRetailPrice());
    }
    
    @Test
    @DisplayName("ProductRequest should still support legacy price field for backward compatibility")
    void testProductRequestWithLegacyPrice() {
        // Given: Create a ProductRequest with legacy price field only
        ProductRequest request = new ProductRequest();
        request.setName("Legacy Product");
        request.setCategory("Furniture");
        request.setSku("LEGACY-001");
        request.setQuantity(5);
        request.setPrice(new BigDecimal("25.00"));
        
        // Then: Legacy price should be accessible
        assertNotNull(request.getPrice());
        assertEquals(new BigDecimal("25.00"), request.getPrice());
        
        // New pricing fields should be null (will be filled from legacy price in controller)
        assertNull(request.getOriginalPrice());
        assertNull(request.getOwnerSetSellPrice());
        assertNull(request.getRetailPrice());
    }
    
    @Test
    @DisplayName("ProductRequest should allow both legacy and new pricing fields")
    void testProductRequestWithBothPricingStyles() {
        // Given: Create a ProductRequest with both pricing styles
        ProductRequest request = new ProductRequest();
        request.setName("Hybrid Product");
        request.setCategory("Books");
        request.setSku("HYBRID-001");
        request.setQuantity(20);
        
        // Set both legacy and new pricing
        request.setPrice(new BigDecimal("30.00"));
        request.setOriginalPrice(new BigDecimal("20.00"));
        request.setOwnerSetSellPrice(new BigDecimal("25.00"));
        request.setRetailPrice(new BigDecimal("35.00"));
        
        // Then: All fields should be accessible
        assertEquals(new BigDecimal("30.00"), request.getPrice());
        assertEquals(new BigDecimal("20.00"), request.getOriginalPrice());
        assertEquals(new BigDecimal("25.00"), request.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("35.00"), request.getRetailPrice());
    }
    
    @Test
    @DisplayName("ProductRequest with no pricing fields should not fail (validation happens at entity level)")
    void testProductRequestWithNoPricing() {
        // Given: Create a ProductRequest without any pricing
        ProductRequest request = new ProductRequest();
        request.setName("No Price Product");
        request.setCategory("Test");
        request.setSku("NOPRICE-001");
        request.setQuantity(1);
        
        // Then: Request should be created but pricing fields are null
        assertNull(request.getPrice());
        assertNull(request.getOriginalPrice());
        assertNull(request.getOwnerSetSellPrice());
        assertNull(request.getRetailPrice());
        
        // Note: Validation will catch this at the entity level when converting to Product
    }
    
    @Test
    @DisplayName("ProductRequest should support partial tiered pricing")
    void testProductRequestWithPartialTieredPricing() {
        // Given: Create a ProductRequest with only some tiered pricing fields
        ProductRequest request = new ProductRequest();
        request.setName("Partial Price Product");
        request.setCategory("Test");
        request.setSku("PARTIAL-001");
        request.setQuantity(5);
        
        // Set only retail price
        request.setRetailPrice(new BigDecimal("50.00"));
        
        // Then: Only retail price should be set
        assertNull(request.getPrice());
        assertNull(request.getOriginalPrice());
        assertNull(request.getOwnerSetSellPrice());
        assertNotNull(request.getRetailPrice());
        assertEquals(new BigDecimal("50.00"), request.getRetailPrice());
    }
}
