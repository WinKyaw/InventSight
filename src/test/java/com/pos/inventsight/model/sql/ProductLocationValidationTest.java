package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Product entity location validation
 * Products must belong to either a store OR a warehouse (or both)
 */
@DisplayName("Product Location Validation Tests")
class ProductLocationValidationTest {

    @Test
    @DisplayName("Product with store only should be valid")
    void testProductWithStoreOnly() {
        // Given
        Company company = createTestCompany();
        Store store = createTestStore(company);
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setSku("TEST-SKU-001");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(100);
        product.setLowStockThreshold(10);
        product.setCompany(company);
        product.setStore(store);
        product.setWarehouse(null); // No warehouse
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> product.validateLocation(),
            "Product with store only should be valid");
    }

    @Test
    @DisplayName("Product with warehouse only should be valid")
    void testProductWithWarehouseOnly() {
        // Given
        Company company = createTestCompany();
        Warehouse warehouse = createTestWarehouse(company);
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setSku("TEST-SKU-002");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(50);
        product.setLowStockThreshold(10);
        product.setCompany(company);
        product.setStore(null); // No store
        product.setWarehouse(warehouse);
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> product.validateLocation(),
            "Product with warehouse only should be valid");
    }

    @Test
    @DisplayName("Product with both store and warehouse should be valid")
    void testProductWithBothStoreAndWarehouse() {
        // Given
        Company company = createTestCompany();
        Store store = createTestStore(company);
        Warehouse warehouse = createTestWarehouse(company);
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setSku("TEST-SKU-003");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(75);
        product.setLowStockThreshold(10);
        product.setCompany(company);
        product.setStore(store);
        product.setWarehouse(warehouse);
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> product.validateLocation(),
            "Product with both store and warehouse should be valid");
    }

    @Test
    @DisplayName("Product with neither store nor warehouse should throw exception")
    void testProductWithoutLocation() {
        // Given
        Company company = createTestCompany();
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setSku("TEST-SKU-004");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(100);
        product.setLowStockThreshold(10);
        product.setCompany(company);
        product.setStore(null); // No store
        product.setWarehouse(null); // No warehouse
        
        // When/Then - should throw exception
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> product.validateLocation(),
            "Product without location should throw exception"
        );
        
        assertEquals("Product must belong to either a store or a warehouse", 
            exception.getMessage());
    }

    // Helper methods to create test entities
    
    private Company createTestCompany() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        return company;
    }
    
    private Store createTestStore(Company company) {
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        store.setAddress("123 Test St");
        store.setCity("Test City");
        store.setState("Test State");
        store.setCountry("Test Country");
        store.setCompany(company);
        return store;
    }
    
    private Warehouse createTestWarehouse(Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Test Warehouse");
        warehouse.setLocation("456 Warehouse Ave");
        warehouse.setCompany(company);
        return warehouse;
    }
}
