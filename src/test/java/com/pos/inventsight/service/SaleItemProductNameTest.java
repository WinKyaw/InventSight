package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that SaleItem correctly stores product name and SKU
 * Addresses issue where items show as "Unknown Item" instead of actual product names
 */
public class SaleItemProductNameTest {

    @Test
    @DisplayName("SaleItem should persist and retrieve productName and productSku from Product")
    public void testSaleItemPersistsProductNameAndSku() {
        // Given: Create a product
        Product product = new Product();
        product.setName("Test Oranges");
        product.setSku("ORG-001");
        product.setPrice(BigDecimal.valueOf(1.99));
        product.setQuantity(100);

        // Given: Create a sale
        Sale sale = new Sale();
        sale.setReceiptNumber("TEST-RECEIPT-001");
        sale.setSubtotal(BigDecimal.valueOf(3.98));
        sale.setTaxAmount(BigDecimal.ZERO);
        sale.setTotalAmount(BigDecimal.valueOf(3.98));
        sale.setStatus(SaleStatus.PENDING);

        // Given: Create a sale item using the constructor
        SaleItem saleItem = new SaleItem(sale, product, 2, BigDecimal.valueOf(1.99));

        // Verify: Constructor should set productName and productSku
        assertEquals("Test Oranges", saleItem.getProductName(), 
            "Constructor should set productName from Product.getName()");
        assertEquals("ORG-001", saleItem.getProductSku(), 
            "Constructor should set productSku from Product.getSku()");
        assertEquals(2, saleItem.getQuantity());
        assertEquals(BigDecimal.valueOf(1.99), saleItem.getUnitPrice());
        assertEquals(BigDecimal.valueOf(3.98), saleItem.getTotalPrice());
    }

    @Test
    @DisplayName("SaleItem productName should be denormalized and independent of Product changes")
    public void testSaleItemProductNameIsDenormalized() {
        // Given: Create a product
        Product product = new Product();
        product.setName("Original Product Name");
        product.setSku("ORIG-SKU");
        product.setPrice(BigDecimal.valueOf(10.00));

        // Given: Create a sale item
        SaleItem saleItem = new SaleItem(
            new Sale(), 
            product, 
            1, 
            BigDecimal.valueOf(10.00)
        );

        // Verify: productName is set
        assertEquals("Original Product Name", saleItem.getProductName());
        assertEquals("ORIG-SKU", saleItem.getProductSku());

        // When: Product name changes
        product.setName("New Product Name");
        product.setSku("NEW-SKU");

        // Then: SaleItem productName should NOT change (denormalized)
        assertEquals("Original Product Name", saleItem.getProductName(), 
            "SaleItem productName should be denormalized and not change when Product changes");
        assertEquals("ORIG-SKU", saleItem.getProductSku(), 
            "SaleItem productSku should be denormalized and not change when Product changes");
    }

    @Test
    @DisplayName("SaleItem should handle null product name and SKU gracefully")
    public void testSaleItemWithNullProductNameAndSku() {
        // Given: Create a product with null name and SKU
        Product product = new Product();
        product.setName(null);
        product.setSku(null);
        product.setPrice(BigDecimal.valueOf(10.00));

        // When: Create a sale item (this might happen with legacy data)
        SaleItem saleItem = new SaleItem(
            new Sale(), 
            product, 
            1, 
            BigDecimal.valueOf(10.00)
        );

        // Then: productName and productSku should be null (not throw exception)
        assertNull(saleItem.getProductName(), 
            "SaleItem should handle null product name gracefully");
        assertNull(saleItem.getProductSku(), 
            "SaleItem should handle null product SKU gracefully");
    }
}
