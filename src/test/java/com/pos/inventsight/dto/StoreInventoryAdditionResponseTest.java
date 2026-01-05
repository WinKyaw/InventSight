package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.StoreInventoryAddition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for store inventory addition response DTO
 * Verifies that product and store information is correctly flattened
 */
class StoreInventoryAdditionResponseTest {

    @Test
    @DisplayName("StoreInventoryAdditionResponse should flatten product and store info")
    void testAdditionResponseFlattensProductInfo() {
        // Arrange
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        Store store = new Store();
        store.setId(storeId);
        store.setStoreName("Main Store");
        
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setSku("TEST-SKU-123");
        
        StoreInventoryAddition addition = new StoreInventoryAddition();
        addition.setId(UUID.randomUUID());
        addition.setStore(store);
        addition.setProduct(product);
        addition.setQuantity(100);
        addition.setUnitCost(new BigDecimal("10.50"));
        addition.setTotalCost(new BigDecimal("1050.00"));
        addition.setSupplierName("Test Supplier");
        addition.setReferenceNumber("REF-001");
        addition.setReceiptDate(LocalDate.now());
        addition.setBatchNumber("BATCH-001");
        addition.setNotes("Test notes");
        addition.setTransactionType(StoreInventoryAddition.TransactionType.RESTOCK);
        addition.setStatus(StoreInventoryAddition.TransactionStatus.COMPLETED);
        addition.setCreatedAt(LocalDateTime.now());
        addition.setCreatedBy("test.user@example.com");
        
        // Act
        StoreInventoryAdditionResponse response = new StoreInventoryAdditionResponse(addition);
        
        // Assert - Verify flattened store info
        assertEquals(storeId, response.getStoreId(), "Store ID should be flattened");
        assertEquals("Main Store", response.getStoreName(), "Store name should be flattened");
        
        // Assert - Verify flattened product info
        assertEquals(productId, response.getProductId(), "Product ID should be flattened");
        assertEquals("Test Product", response.getProductName(), "Product name should be flattened");
        assertEquals("TEST-SKU-123", response.getProductSku(), "Product SKU should be flattened");
        
        // Assert - Verify other fields are preserved
        assertEquals(100, response.getQuantity(), "Quantity should be preserved");
        assertEquals(new BigDecimal("10.50"), response.getUnitCost(), "Unit cost should be preserved");
        assertEquals(new BigDecimal("1050.00"), response.getTotalCost(), "Total cost should be preserved");
        assertEquals("Test Supplier", response.getSupplierName(), "Supplier name should be preserved");
        assertEquals("REF-001", response.getReferenceNumber(), "Reference number should be preserved");
        assertEquals("BATCH-001", response.getBatchNumber(), "Batch number should be preserved");
        assertEquals("Test notes", response.getNotes(), "Notes should be preserved");
        assertEquals("RESTOCK", response.getTransactionType(), "Transaction type should be converted to string");
        assertEquals("COMPLETED", response.getStatus(), "Status should be converted to string");
        assertEquals("test.user@example.com", response.getCreatedBy(), "Created by should be preserved");
    }

    @Test
    @DisplayName("StoreInventoryAdditionResponse should handle null product gracefully")
    void testAdditionResponseHandlesNullProduct() {
        // Arrange
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        
        StoreInventoryAddition addition = new StoreInventoryAddition();
        addition.setId(UUID.randomUUID());
        addition.setStore(store);
        addition.setProduct(null);  // Simulate deleted product
        addition.setQuantity(50);
        addition.setCreatedBy("test.user@example.com");
        
        // Act
        StoreInventoryAdditionResponse response = new StoreInventoryAdditionResponse(addition);
        
        // Assert
        assertNull(response.getProductId(), "Product ID should be null");
        assertEquals("Unknown Product", response.getProductName(), "Product name should be 'Unknown Product'");
        assertNull(response.getProductSku(), "Product SKU should be null");
    }

    @Test
    @DisplayName("StoreInventoryAdditionResponse should handle null store gracefully")
    void testAdditionResponseHandlesNullStore() {
        // Arrange
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setSku("TEST-SKU-456");
        
        StoreInventoryAddition addition = new StoreInventoryAddition();
        addition.setId(UUID.randomUUID());
        addition.setStore(null);  // Simulate deleted store
        addition.setProduct(product);
        addition.setQuantity(25);
        addition.setCreatedBy("admin@example.com");
        
        // Act
        StoreInventoryAdditionResponse response = new StoreInventoryAdditionResponse(addition);
        
        // Assert
        assertNull(response.getStoreId(), "Store ID should be null");
        assertNull(response.getStoreName(), "Store name should be null");
        assertEquals(product.getId(), response.getProductId(), "Product ID should be preserved");
        assertEquals("Test Product", response.getProductName(), "Product name should be preserved");
    }

    @Test
    @DisplayName("StoreInventoryAdditionResponse should calculate total cost from unit cost and quantity")
    void testAdditionEntityCalculatesTotalCost() {
        // Arrange
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        
        StoreInventoryAddition addition = new StoreInventoryAddition(store, product, 10);
        addition.setUnitCost(new BigDecimal("15.00"));
        
        // Act
        addition.calculateTotalCost();
        
        // Assert
        assertEquals(new BigDecimal("150.00"), addition.getTotalCost(), "Total cost should be unit cost * quantity");
    }
}
