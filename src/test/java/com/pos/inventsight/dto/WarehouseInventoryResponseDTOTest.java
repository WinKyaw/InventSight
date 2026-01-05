package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for warehouse inventory response DTOs
 * Verifies that product and warehouse information is correctly flattened
 */
class WarehouseInventoryResponseDTOTest {

    @Test
    @DisplayName("WarehouseInventoryAdditionResponse should flatten product and warehouse info")
    void testAdditionResponseFlattensProductInfo() {
        // Arrange
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Main Warehouse");
        
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setSku("TEST-SKU-123");
        
        WarehouseInventoryAddition addition = new WarehouseInventoryAddition();
        addition.setId(UUID.randomUUID());
        addition.setWarehouse(warehouse);
        addition.setProduct(product);
        addition.setQuantity(100);
        addition.setUnitCost(new BigDecimal("10.50"));
        addition.setTotalCost(new BigDecimal("1050.00"));
        addition.setSupplierName("Test Supplier");
        addition.setReferenceNumber("REF-001");
        addition.setReceiptDate(LocalDate.now());
        addition.setBatchNumber("BATCH-001");
        addition.setNotes("Test notes");
        addition.setTransactionType(WarehouseInventoryAddition.TransactionType.RECEIPT);
        addition.setStatus(WarehouseInventoryAddition.TransactionStatus.COMPLETED);
        addition.setCreatedAt(LocalDateTime.now());
        addition.setCreatedBy("test.user@example.com");
        
        // Act
        WarehouseInventoryAdditionResponse response = new WarehouseInventoryAdditionResponse(addition);
        
        // Assert - Verify flattened warehouse info
        assertEquals(warehouseId, response.getWarehouseId(), "Warehouse ID should be flattened");
        assertEquals("Main Warehouse", response.getWarehouseName(), "Warehouse name should be flattened");
        
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
        assertEquals("RECEIPT", response.getTransactionType(), "Transaction type should be converted to string");
        assertEquals("COMPLETED", response.getStatus(), "Status should be converted to string");
        assertEquals("test.user@example.com", response.getCreatedBy(), "Created by should be preserved");
        
        System.out.println("✅ WarehouseInventoryAdditionResponse correctly flattens product and warehouse info");
    }

    @Test
    @DisplayName("WarehouseInventoryAdditionResponse should handle null product gracefully")
    void testAdditionResponseHandlesNullProduct() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Test Warehouse");
        
        WarehouseInventoryAddition addition = new WarehouseInventoryAddition();
        addition.setId(UUID.randomUUID());
        addition.setWarehouse(warehouse);
        addition.setProduct(null);  // Simulate deleted product
        addition.setQuantity(50);
        addition.setCreatedBy("test.user@example.com");
        
        // Act
        WarehouseInventoryAdditionResponse response = new WarehouseInventoryAdditionResponse(addition);
        
        // Assert
        assertNull(response.getProductId(), "Product ID should be null");
        assertEquals("Unknown Product", response.getProductName(), "Product name should be 'Unknown Product'");
        assertNull(response.getProductSku(), "Product SKU should be null");
        
        System.out.println("✅ WarehouseInventoryAdditionResponse handles null product gracefully");
    }

    @Test
    @DisplayName("WarehouseInventoryWithdrawalResponse should flatten product and warehouse info")
    void testWithdrawalResponseFlattensProductInfo() {
        // Arrange
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Branch Warehouse");
        
        Product product = new Product();
        product.setId(productId);
        product.setName("Withdrawal Product");
        product.setSku("WDR-SKU-456");
        
        WarehouseInventoryWithdrawal withdrawal = new WarehouseInventoryWithdrawal();
        withdrawal.setId(UUID.randomUUID());
        withdrawal.setWarehouse(warehouse);
        withdrawal.setProduct(product);
        withdrawal.setQuantity(25);
        withdrawal.setUnitCost(new BigDecimal("5.00"));
        withdrawal.setTotalCost(new BigDecimal("125.00"));
        withdrawal.setDestination("Store A");
        withdrawal.setReferenceNumber("WDR-001");
        withdrawal.setWithdrawalDate(LocalDate.now());
        withdrawal.setReason("Transfer to store");
        withdrawal.setNotes("Transfer notes");
        withdrawal.setTransactionType(WarehouseInventoryWithdrawal.TransactionType.ISSUE);
        withdrawal.setStatus(WarehouseInventoryWithdrawal.TransactionStatus.COMPLETED);
        withdrawal.setCreatedAt(LocalDateTime.now());
        withdrawal.setCreatedBy("admin@example.com");
        
        // Act
        WarehouseInventoryWithdrawalResponse response = new WarehouseInventoryWithdrawalResponse(withdrawal);
        
        // Assert - Verify flattened warehouse info
        assertEquals(warehouseId, response.getWarehouseId(), "Warehouse ID should be flattened");
        assertEquals("Branch Warehouse", response.getWarehouseName(), "Warehouse name should be flattened");
        
        // Assert - Verify flattened product info
        assertEquals(productId, response.getProductId(), "Product ID should be flattened");
        assertEquals("Withdrawal Product", response.getProductName(), "Product name should be flattened");
        assertEquals("WDR-SKU-456", response.getProductSku(), "Product SKU should be flattened");
        
        // Assert - Verify other fields are preserved
        assertEquals(25, response.getQuantity(), "Quantity should be preserved");
        assertEquals(new BigDecimal("5.00"), response.getUnitCost(), "Unit cost should be preserved");
        assertEquals(new BigDecimal("125.00"), response.getTotalCost(), "Total cost should be preserved");
        assertEquals("Store A", response.getDestination(), "Destination should be preserved");
        assertEquals("WDR-001", response.getReferenceNumber(), "Reference number should be preserved");
        assertEquals("Transfer to store", response.getReason(), "Reason should be preserved");
        assertEquals("Transfer notes", response.getNotes(), "Notes should be preserved");
        assertEquals("ISSUE", response.getTransactionType(), "Transaction type should be converted to string");
        assertEquals("COMPLETED", response.getStatus(), "Status should be converted to string");
        assertEquals("admin@example.com", response.getCreatedBy(), "Created by should be preserved");
        
        System.out.println("✅ WarehouseInventoryWithdrawalResponse correctly flattens product and warehouse info");
    }

    @Test
    @DisplayName("WarehouseInventoryWithdrawalResponse should handle null product gracefully")
    void testWithdrawalResponseHandlesNullProduct() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Test Warehouse");
        
        WarehouseInventoryWithdrawal withdrawal = new WarehouseInventoryWithdrawal();
        withdrawal.setId(UUID.randomUUID());
        withdrawal.setWarehouse(warehouse);
        withdrawal.setProduct(null);  // Simulate deleted product
        withdrawal.setQuantity(10);
        withdrawal.setCreatedBy("test.user@example.com");
        
        // Act
        WarehouseInventoryWithdrawalResponse response = new WarehouseInventoryWithdrawalResponse(withdrawal);
        
        // Assert
        assertNull(response.getProductId(), "Product ID should be null");
        assertEquals("Unknown Product", response.getProductName(), "Product name should be 'Unknown Product'");
        assertNull(response.getProductSku(), "Product SKU should be null");
        
        System.out.println("✅ WarehouseInventoryWithdrawalResponse handles null product gracefully");
    }
}
