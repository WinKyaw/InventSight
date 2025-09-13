package com.pos.inventsight.uuid;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.SaleItem;
import com.pos.inventsight.model.sql.Sale;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify UUID relationships work correctly between entities
 */
public class UuidRelationshipTest {

    @Test
    public void testProductStoreUuidRelationship() {
        // Create store with UUID
        Store store = new Store("UUID Test Store", "123 UUID St", "UUID City", "UC", "UUID Country");
        
        // Create product with store relationship
        Product product = new Product("UUID Test Product", "UUID-001", 
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.00), 
                BigDecimal.valueOf(15.00), 100, store);
        
        // Simulate what Hibernate would do
        store.setId(UUID.randomUUID());
        product.setId(UUID.randomUUID());
        
        // Verify UUIDs are generated
        assertNotNull(store.getId());
        assertNotNull(product.getId());
        assertTrue(store.getId() instanceof UUID);
        assertTrue(product.getId() instanceof UUID);
        
        // Verify relationship
        assertEquals(store, product.getStore());
        assertEquals(store.getId(), product.getStore().getId());
    }

    @Test
    public void testSaleItemProductUuidRelationship() {
        // Create store and product with UUIDs
        Store store = new Store("Sale Test Store", "456 Sale St", "Sale City", "SC", "Sale Country");
        Product product = new Product("Sale Test Product", "SALE-001", 
                BigDecimal.valueOf(20.00), BigDecimal.valueOf(25.00), 
                BigDecimal.valueOf(30.00), 50, store);
        
        // Simulate what Hibernate would do
        store.setId(UUID.randomUUID());
        product.setId(UUID.randomUUID());
        
        // Create sale
        Sale sale = new Sale();
        sale.setSubtotal(BigDecimal.valueOf(30.00));
        sale.setTaxAmount(BigDecimal.valueOf(3.00));
        sale.setTotalAmount(BigDecimal.valueOf(33.00));
        sale.setStore(store);
        
        // Create sale item with product relationship
        SaleItem saleItem = new SaleItem(sale, product, 1, BigDecimal.valueOf(30.00));
        
        // Verify UUIDs
        assertNotNull(store.getId());
        assertNotNull(product.getId());
        assertTrue(store.getId() instanceof UUID);
        assertTrue(product.getId() instanceof UUID);
        
        // Verify relationships
        assertEquals(product, saleItem.getProduct());
        assertEquals(product.getId(), saleItem.getProduct().getId());
        assertEquals(store, saleItem.getProduct().getStore());
        assertEquals(store.getId(), saleItem.getProduct().getStore().getId());
        
        // Verify sale item properties
        assertEquals("Sale Test Product", saleItem.getProductName());
        assertEquals("SALE-001", saleItem.getProductSku());
        assertEquals(BigDecimal.valueOf(30.00), saleItem.getTotalPrice());
    }

    @Test
    public void testUuidStringConversion() {
        // Create entities with UUIDs
        Store store = new Store("String Test Store", "789 String St", "String City", "SC", "String Country");
        Product product = new Product("String Test Product", "STR-001", 
                BigDecimal.valueOf(5.00), BigDecimal.valueOf(7.00), 
                BigDecimal.valueOf(10.00), 200, store);
        
        // Simulate what Hibernate would do
        store.setId(UUID.randomUUID());
        product.setId(UUID.randomUUID());
        
        // Test UUID string conversion
        String storeUuidString = store.getId().toString();
        String productUuidString = product.getId().toString();
        
        // Verify string format
        assertTrue(storeUuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
        assertTrue(productUuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
        
        // Test conversion back from string
        UUID storeUuidFromString = UUID.fromString(storeUuidString);
        UUID productUuidFromString = UUID.fromString(productUuidString);
        
        assertEquals(store.getId(), storeUuidFromString);
        assertEquals(product.getId(), productUuidFromString);
    }

    @Test
    public void testEntityWithNullStore() {
        // Create product without store initially
        Product product = new Product();
        product.setName("No Store Product");
        product.setSku("NOSTORE-001");
        product.setOriginalPrice(BigDecimal.valueOf(1.00));
        product.setOwnerSetSellPrice(BigDecimal.valueOf(2.00));
        product.setRetailPrice(BigDecimal.valueOf(3.00));
        product.setQuantity(10);
        
        // Simulate what Hibernate would do
        product.setId(UUID.randomUUID());
        
        // Verify UUID is generated after simulated persistence
        assertNotNull(product.getId());
        assertTrue(product.getId() instanceof UUID);
        
        // Store should be null initially
        assertNull(product.getStore());
        
        // Add store later
        Store store = new Store("Later Added Store", "999 Later St", "Later City", "LC", "Later Country");
        store.setId(UUID.randomUUID()); // Simulate persisted state
        product.setStore(store);
        
        // Verify relationship
        assertEquals(store, product.getStore());
        assertEquals(store.getId(), product.getStore().getId());
    }
}