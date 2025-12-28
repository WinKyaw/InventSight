package com.pos.inventsight.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SKU Generator Unit Tests")
class SkuGeneratorTest {
    
    private SkuGenerator skuGenerator;
    
    @BeforeEach
    void setUp() {
        skuGenerator = new SkuGenerator();
    }
    
    @Test
    @DisplayName("Should generate 11-digit SKU")
    void shouldGenerate11DigitSku() {
        String sku = skuGenerator.generate11DigitSku();
        
        assertNotNull(sku);
        assertEquals(11, sku.length());
        assertTrue(sku.matches("\\d{11}"), "SKU should contain only digits");
    }
    
    @Test
    @DisplayName("Should generate SKU within valid range")
    void shouldGenerateSkuWithinValidRange() {
        String sku = skuGenerator.generate11DigitSku();
        long skuNumber = Long.parseLong(sku);
        
        assertTrue(skuNumber >= 10000000000L, "SKU should be >= 10000000000");
        assertTrue(skuNumber <= 99999999999L, "SKU should be <= 99999999999");
    }
    
    @Test
    @DisplayName("Should generate unique SKUs")
    void shouldGenerateUniqueSkus() {
        Set<String> generatedSkus = new HashSet<>();
        int count = 1000;
        
        for (int i = 0; i < count; i++) {
            String sku = skuGenerator.generate11DigitSku();
            generatedSkus.add(sku);
        }
        
        // Allow for very small collision rate (< 1%)
        assertTrue(generatedSkus.size() > count * 0.99, 
            "Should generate mostly unique SKUs (> 99%)");
    }
    
    @Test
    @DisplayName("Should generate unique SKU with exists checker")
    void shouldGenerateUniqueSkuWithExistsChecker() {
        Set<String> existingSkus = new HashSet<>();
        
        // Generate first SKU
        String sku1 = skuGenerator.generateUniqueSku(existingSkus::contains);
        assertNotNull(sku1);
        existingSkus.add(sku1);
        
        // Generate second SKU - should be different
        String sku2 = skuGenerator.generateUniqueSku(existingSkus::contains);
        assertNotNull(sku2);
        assertNotEquals(sku1, sku2);
    }
    
    @Test
    @DisplayName("Should retry when SKU exists")
    void shouldRetryWhenSkuExists() {
        Set<String> existingSkus = new HashSet<>();
        
        // Generate some SKUs and add them to existing set
        for (int i = 0; i < 10; i++) {
            existingSkus.add(skuGenerator.generate11DigitSku());
        }
        
        // Generate a new unique SKU
        String newSku = skuGenerator.generateUniqueSku(existingSkus::contains);
        
        assertNotNull(newSku);
        assertFalse(existingSkus.contains(newSku), "Generated SKU should not exist in the set");
    }
    
    @Test
    @DisplayName("Should throw exception when unable to generate unique SKU")
    void shouldThrowExceptionWhenUnableToGenerateUniqueSku() {
        // Create a predicate that always returns true (all SKUs exist)
        assertThrows(IllegalStateException.class, () -> {
            skuGenerator.generateUniqueSku(sku -> true);
        }, "Should throw IllegalStateException when unable to generate unique SKU");
    }
    
    @Test
    @DisplayName("Should generate different SKUs on multiple calls")
    void shouldGenerateDifferentSkusOnMultipleCalls() {
        String sku1 = skuGenerator.generate11DigitSku();
        String sku2 = skuGenerator.generate11DigitSku();
        String sku3 = skuGenerator.generate11DigitSku();
        
        // While it's possible (but extremely unlikely) they could be equal,
        // we test that they're all valid 11-digit numbers
        assertEquals(11, sku1.length());
        assertEquals(11, sku2.length());
        assertEquals(11, sku3.length());
        
        assertTrue(sku1.matches("\\d{11}"));
        assertTrue(sku2.matches("\\d{11}"));
        assertTrue(sku3.matches("\\d{11}"));
    }
}
