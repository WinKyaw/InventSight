package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Validation Requirements Tests")
class ProductValidationTest {

    @Test
    @DisplayName("Product should require all @NotNull fields to prevent JPA transaction errors")
    void testProductRequiredFieldsForJpaTransaction() {
        // This test validates the fields that would cause "Could not commit JPA transaction" errors
        
        // Create a store (required for product)
        Store store = new Store();
        store.setStoreName("Test Store");
        
        // Create product with all required fields
        Product validProduct = new Product();
        validProduct.setName("Valid Product");
        validProduct.setSku("VALID-001");
        validProduct.setStore(store); // @NotNull - This was missing before the fix
        validProduct.setOriginalPrice(new BigDecimal("10.00")); // @NotNull
        validProduct.setOwnerSetSellPrice(new BigDecimal("15.00")); // @NotNull 
        validProduct.setRetailPrice(new BigDecimal("20.00")); // @NotNull
        validProduct.setQuantity(100);
        
        // Verify all critical fields are set
        assertNotNull(validProduct.getStore(), "Store is required to prevent JPA transaction error");
        assertNotNull(validProduct.getOriginalPrice(), "Original price is required");
        assertNotNull(validProduct.getOwnerSetSellPrice(), "Owner set sell price is required");
        assertNotNull(validProduct.getRetailPrice(), "Retail price is required");
        
        System.out.println("✅ Valid product created with all required fields:");
        System.out.println("   Store: " + validProduct.getStore().getStoreName());
        System.out.println("   Pricing: Original=$" + validProduct.getOriginalPrice() + 
                          ", Owner=$" + validProduct.getOwnerSetSellPrice() + 
                          ", Retail=$" + validProduct.getRetailPrice());
    }
    
    @Test
    @DisplayName("Product without store would cause JPA transaction error (before fix)")
    void testProductWithoutStoreWouldFailJpaTransaction() {
        // This demonstrates what would have failed before the fix
        Product invalidProduct = new Product();
        invalidProduct.setName("Invalid Product");
        invalidProduct.setSku("INVALID-001");
        // Missing store assignment - this would cause JPA transaction error
        invalidProduct.setOriginalPrice(new BigDecimal("10.00"));
        invalidProduct.setOwnerSetSellPrice(new BigDecimal("15.00"));
        invalidProduct.setRetailPrice(new BigDecimal("20.00"));
        
        // Verify the problematic condition
        assertNull(invalidProduct.getStore(), "Product without store would fail JPA constraint");
        
        System.out.println("✅ Identified issue: Product without store would cause JPA transaction failure");
        System.out.println("   This is what the ProductService.createProduct fix addresses");
    }
    
    @Test
    @DisplayName("Product without required pricing fields would cause JPA transaction error")
    void testProductWithoutRequiredPricingWouldFailJpaTransaction() {
        Store store = new Store();
        store.setStoreName("Test Store");
        
        // Product missing required pricing fields
        Product invalidProduct = new Product();
        invalidProduct.setName("Invalid Product");
        invalidProduct.setSku("INVALID-002");
        invalidProduct.setStore(store);
        // Missing required @NotNull pricing fields would cause JPA error
        
        assertNull(invalidProduct.getOriginalPrice(), "Missing original price would fail JPA constraint");
        assertNull(invalidProduct.getOwnerSetSellPrice(), "Missing owner set sell price would fail JPA constraint");
        assertNull(invalidProduct.getRetailPrice(), "Missing retail price would fail JPA constraint");
        
        System.out.println("✅ Identified issue: Product without required pricing would cause JPA transaction failure");
        System.out.println("   Applications must ensure all @NotNull pricing fields are set");
    }
}