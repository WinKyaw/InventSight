package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Entity Business Logic Tests")
class ProductEntityValidationTest {

    @Test
    @DisplayName("Product should generate UUID and handle business operations correctly")
    void testProductUuidAndBusinessLogic() {
        // Create store
        Store store = new Store();
        store.setStoreName("Test Store");
        store.setAddress("123 Test St");
        store.setCity("Test City");
        store.setState("Test State");
        store.setCountry("Test Country");
        
        // Verify store has UUID
        assertNotNull(store.getId());
        assertTrue(store.getId() instanceof UUID);
        
        // Create product
        Product product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-SKU-001");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("8.00"));
        product.setQuantity(100);
        product.setLowStockThreshold(10);
        product.setReorderLevel(5);
        product.setStore(store);
        
        // Verify product has UUID
        assertNotNull(product.getId());
        assertTrue(product.getId() instanceof UUID);
        
        // Test UUID string format
        String productUuidString = product.getId().toString();
        assertTrue(productUuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
        
        String storeUuidString = store.getId().toString();
        assertTrue(storeUuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
        
        // Test business logic works with UUID entities
        assertFalse(product.isLowStock()); // 100 > 10
        assertFalse(product.isOutOfStock()); // 100 > 0
        assertFalse(product.needsReorder()); // 100 > 5
        
        // Test profit calculations
        BigDecimal expectedRetailProfit = new BigDecimal("12.00"); // 20.00 - 8.00
        assertEquals(expectedRetailProfit, product.getRetailProfit());
        
        BigDecimal expectedOwnerProfit = new BigDecimal("2.00"); // 10.00 - 8.00
        assertEquals(expectedOwnerProfit, product.getOwnerProfit());
        
        // Test tiered pricing
        assertEquals(product.getOriginalPrice(), product.getPriceForRole(UserRole.OWNER));
        assertEquals(product.getOwnerSetSellPrice(), product.getPriceForRole(UserRole.MANAGER));
        assertEquals(product.getRetailPrice(), product.getPriceForRole(UserRole.EMPLOYEE));
        
        // Test store relationship
        assertEquals(store.getId(), product.getStore().getId());
        assertEquals("Test Store", product.getStore().getStoreName());
    }
    
    @Test
    @DisplayName("Multiple entities should have unique UUIDs")
    void testUniqueUuids() {
        Store store1 = new Store();
        Store store2 = new Store();
        Product product1 = new Product();
        Product product2 = new Product();
        
        // All UUIDs should be unique
        assertNotEquals(store1.getId(), store2.getId());
        assertNotEquals(product1.getId(), product2.getId());
        assertNotEquals(store1.getId(), product1.getId());
        assertNotEquals(store2.getId(), product2.getId());
    }
    
    @Test
    @DisplayName("SaleItem should work with UUID Product reference")
    void testSaleItemWithUuidProduct() {
        // Create store and product
        Store store = new Store();
        store.setStoreName("Test Store");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(50);
        product.setStore(store);
        
        // Create sale
        Sale sale = new Sale();
        sale.setReceiptNumber("RECEIPT-001");
        sale.setSubtotal(new BigDecimal("20.00"));
        sale.setTaxAmount(new BigDecimal("2.00"));
        sale.setTotalAmount(new BigDecimal("22.00"));
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentMethod(PaymentMethod.CASH);
        
        // Create sale item
        SaleItem saleItem = new SaleItem(sale, product, 1, product.getRetailPrice());
        
        // Verify relationships work with UUIDs
        assertNotNull(saleItem.getProduct());
        assertEquals(product.getId(), saleItem.getProduct().getId());
        assertTrue(saleItem.getProduct().getId() instanceof UUID);
        assertEquals("Test Product", saleItem.getProductName());
        assertEquals("TEST-001", saleItem.getProductSku());
        
        // Verify calculations work
        assertEquals(new BigDecimal("20.00"), saleItem.getTotalPrice());
    }
    
    @Test
    @DisplayName("DiscountAuditLog should work with UUID references")
    void testDiscountAuditLogWithUuidReferences() {
        // Create user (uses Long ID but has UUID)
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        
        // Create store and product
        Store store = new Store();
        store.setStoreName("Test Store");
        
        Product product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(50);
        product.setStore(store);
        
        // Create discount audit log
        DiscountAuditLog auditLog = new DiscountAuditLog();
        auditLog.setUser(user);
        auditLog.setRole(UserRole.EMPLOYEE);
        auditLog.setStore(store);
        auditLog.setProduct(product);
        auditLog.setAttemptedPrice(new BigDecimal("18.00"));
        auditLog.setOriginalPrice(product.getRetailPrice());
        auditLog.setResult(DiscountResult.APPROVED);
        
        // Verify UUID relationships work
        assertNotNull(auditLog.getProduct());
        assertNotNull(auditLog.getStore());
        assertEquals(product.getId(), auditLog.getProduct().getId());
        assertEquals(store.getId(), auditLog.getStore().getId());
        assertTrue(auditLog.getProduct().getId() instanceof UUID);
        assertTrue(auditLog.getStore().getId() instanceof UUID);
        
        // Verify business logic works
        assertEquals(new BigDecimal("2.00"), auditLog.getDiscountAmount()); // 20.00 - 18.00
        assertTrue(auditLog.isApproved());
    }
}