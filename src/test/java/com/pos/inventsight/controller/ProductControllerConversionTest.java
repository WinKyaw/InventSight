package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ProductRequest;
import com.pos.inventsight.model.sql.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify ProductRequest to Product entity conversion
 * focusing on the tiered pricing fields fix
 */
class ProductControllerConversionTest {
    
    private ProductController controller;
    
    @BeforeEach
    void setUp() {
        controller = new ProductController();
    }
    
    @Test
    void testConvertFromRequest_WithTieredPricing() {
        // Given: A ProductRequest with tiered pricing fields
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setSku("TEST-001");
        request.setCategory("Electronics");
        request.setQuantity(10);
        request.setOriginalPrice(new BigDecimal("10.00"));
        request.setOwnerSetSellPrice(new BigDecimal("15.00"));
        request.setRetailPrice(new BigDecimal("20.00"));
        request.setCostPrice(new BigDecimal("8.00"));
        
        // When: Converting to Product entity
        Product product = (Product) ReflectionTestUtils.invokeMethod(
            controller, "convertFromRequest", request
        );
        
        // Then: All pricing fields should be properly set
        assertNotNull(product);
        assertEquals("Test Product", product.getName());
        assertEquals("TEST-001", product.getSku());
        assertEquals("Electronics", product.getCategory());
        assertEquals(Integer.valueOf(10), product.getQuantity());
        
        // Verify all three required pricing fields are set
        assertNotNull(product.getOriginalPrice(), "Original price should not be null");
        assertNotNull(product.getOwnerSetSellPrice(), "Owner set sell price should not be null");
        assertNotNull(product.getRetailPrice(), "Retail price should not be null");
        
        assertEquals(new BigDecimal("10.00"), product.getOriginalPrice());
        assertEquals(new BigDecimal("15.00"), product.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("20.00"), product.getRetailPrice());
        assertEquals(new BigDecimal("8.00"), product.getCostPrice());
        
        // Legacy price should be set to retail price
        assertEquals(new BigDecimal("20.00"), product.getPrice());
    }
    
    @Test
    void testConvertFromRequest_WithLegacyPrice() {
        // Given: A ProductRequest with legacy price field only (backward compatibility)
        ProductRequest request = new ProductRequest();
        request.setName("Legacy Product");
        request.setSku("LEGACY-001");
        request.setCategory("Furniture");
        request.setQuantity(5);
        request.setPrice(new BigDecimal("25.00"));
        request.setCostPrice(new BigDecimal("15.00"));
        
        // When: Converting to Product entity
        Product product = (Product) ReflectionTestUtils.invokeMethod(
            controller, "convertFromRequest", request
        );
        
        // Then: All three pricing fields should be set to the legacy price value
        assertNotNull(product);
        assertEquals("Legacy Product", product.getName());
        
        // Verify backward compatibility: all three fields set from legacy price
        assertNotNull(product.getOriginalPrice(), "Original price should be set from legacy price");
        assertNotNull(product.getOwnerSetSellPrice(), "Owner set sell price should be set from legacy price");
        assertNotNull(product.getRetailPrice(), "Retail price should be set from legacy price");
        
        assertEquals(new BigDecimal("25.00"), product.getOriginalPrice());
        assertEquals(new BigDecimal("25.00"), product.getOwnerSetSellPrice());
        assertEquals(new BigDecimal("25.00"), product.getRetailPrice());
        assertEquals(new BigDecimal("25.00"), product.getPrice());
    }
    
    @Test
    void testConvertFromRequest_WithMixedPricing() {
        // Given: A ProductRequest with some new and some legacy fields
        ProductRequest request = new ProductRequest();
        request.setName("Mixed Product");
        request.setSku("MIXED-001");
        request.setCategory("Books");
        request.setQuantity(20);
        request.setPrice(new BigDecimal("12.00"));
        request.setRetailPrice(new BigDecimal("18.00"));
        // originalPrice and ownerSetSellPrice not provided
        
        // When: Converting to Product entity
        Product product = (Product) ReflectionTestUtils.invokeMethod(
            controller, "convertFromRequest", request
        );
        
        // Then: Missing fields should be set from legacy price
        assertNotNull(product);
        assertEquals(new BigDecimal("12.00"), product.getOriginalPrice(), 
            "Original price should fall back to legacy price");
        assertEquals(new BigDecimal("12.00"), product.getOwnerSetSellPrice(), 
            "Owner set sell price should fall back to legacy price");
        assertEquals(new BigDecimal("18.00"), product.getRetailPrice(), 
            "Retail price should use provided value");
        assertEquals(new BigDecimal("12.00"), product.getPrice(), 
            "Legacy price should use provided value");
    }
    
    @Test
    void testConvertFromRequest_TieredPricingTakesPrecedence() {
        // Given: A ProductRequest with both legacy and new pricing fields
        ProductRequest request = new ProductRequest();
        request.setName("Precedence Product");
        request.setSku("PREC-001");
        request.setCategory("Toys");
        request.setQuantity(15);
        request.setPrice(new BigDecimal("30.00")); // Legacy
        request.setOriginalPrice(new BigDecimal("20.00")); // New
        request.setOwnerSetSellPrice(new BigDecimal("25.00")); // New
        request.setRetailPrice(new BigDecimal("35.00")); // New
        
        // When: Converting to Product entity
        Product product = (Product) ReflectionTestUtils.invokeMethod(
            controller, "convertFromRequest", request
        );
        
        // Then: New pricing fields should take precedence over legacy price
        assertNotNull(product);
        assertEquals(new BigDecimal("20.00"), product.getOriginalPrice(), 
            "Original price should use new field, not legacy");
        assertEquals(new BigDecimal("25.00"), product.getOwnerSetSellPrice(), 
            "Owner set sell price should use new field, not legacy");
        assertEquals(new BigDecimal("35.00"), product.getRetailPrice(), 
            "Retail price should use new field, not legacy");
        assertEquals(new BigDecimal("30.00"), product.getPrice(), 
            "Legacy price should still be set for backward compatibility");
    }
    
    @Test
    void testConvertFromRequest_AllOtherFieldsPreserved() {
        // Given: A ProductRequest with various other fields
        ProductRequest request = new ProductRequest();
        request.setName("Complete Product");
        request.setSku("COMP-001");
        request.setCategory("Sports");
        request.setDescription("A complete product with all fields");
        request.setQuantity(50);
        request.setMaxQuantity(100);
        request.setUnit("pieces");
        request.setSupplier("Supplier Inc");
        request.setLocation("Warehouse A");
        request.setBarcode("123456789");
        request.setLowStockThreshold(10);
        request.setReorderLevel(20);
        request.setOriginalPrice(new BigDecimal("5.00"));
        request.setOwnerSetSellPrice(new BigDecimal("8.00"));
        request.setRetailPrice(new BigDecimal("10.00"));
        
        // When: Converting to Product entity
        Product product = (Product) ReflectionTestUtils.invokeMethod(
            controller, "convertFromRequest", request
        );
        
        // Then: All fields should be properly mapped
        assertNotNull(product);
        assertEquals("Complete Product", product.getName());
        assertEquals("COMP-001", product.getSku());
        assertEquals("Sports", product.getCategory());
        assertEquals("A complete product with all fields", product.getDescription());
        assertEquals(Integer.valueOf(50), product.getQuantity());
        assertEquals(Integer.valueOf(100), product.getMaxQuantity());
        assertEquals("pieces", product.getUnit());
        assertEquals("Supplier Inc", product.getSupplier());
        assertEquals("Warehouse A", product.getLocation());
        assertEquals("123456789", product.getBarcode());
        assertEquals(Integer.valueOf(10), product.getLowStockThreshold());
        assertEquals(Integer.valueOf(20), product.getReorderLevel());
    }
}
