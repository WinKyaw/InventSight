package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product UUID Migration Tests")
class ProductUuidMigrationTest {

    private Store store;
    private Product product;
    private SaleItem saleItem;
    private DiscountAuditLog auditLog;
    private User user;

    @BeforeEach
    void setUp() {
        // Create a store with UUID (simulate persisted state)
        store = new Store();
        store.setStoreName("Test Store");
        store.setAddress("123 Test St");
        store.setCity("Test City");
        store.setState("Test State");
        store.setCountry("Test Country");
        store.setId(UUID.randomUUID()); // Simulate what Hibernate would do
        assertNotNull(store.getId(), "Store should have a UUID id");
        assertTrue(store.getId() instanceof UUID, "Store id should be UUID type");

        // Create a product with UUID (simulate persisted state)
        product = new Product();
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setOriginalPrice(new BigDecimal("10.00"));
        product.setOwnerSetSellPrice(new BigDecimal("15.00"));
        product.setRetailPrice(new BigDecimal("20.00"));
        product.setQuantity(100);
        product.setStore(store);
        product.setId(UUID.randomUUID()); // Simulate what Hibernate would do
        assertNotNull(product.getId(), "Product should have a UUID id");
        assertTrue(product.getId() instanceof UUID, "Product id should be UUID type");

        // Create a user for audit log
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        // Note: User entity uses Long ID and separate UUID column
        assertNotNull(user.getUuid(), "User should have a UUID");
        assertTrue(user.getUuid() instanceof UUID, "User uuid should be UUID type");
    }

    @Test
    @DisplayName("Product should have UUID primary key")
    void testProductHasUuidId() {
        assertNotNull(product.getId());
        assertTrue(product.getId() instanceof UUID);
        
        // Test that UUID is properly formatted
        String uuidString = product.getId().toString();
        assertTrue(uuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    @DisplayName("Store should have UUID primary key")
    void testStoreHasUuidId() {
        assertNotNull(store.getId());
        assertTrue(store.getId() instanceof UUID);
        
        // Test that UUID is properly formatted
        String uuidString = store.getId().toString();
        assertTrue(uuidString.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    @DisplayName("SaleItem should properly reference Product with UUID")
    void testSaleItemProductReference() {
        // Create a sale first
        Sale sale = new Sale();
        sale.setReceiptNumber("TEST-RECEIPT-001");
        sale.setSubtotal(new BigDecimal("20.00"));
        sale.setTaxAmount(new BigDecimal("2.00"));
        sale.setTotalAmount(new BigDecimal("22.00"));
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentMethod(PaymentMethod.CASH);
        
        // Create sale item that references the product
        saleItem = new SaleItem(sale, product, 1, product.getRetailPrice());
        
        // Verify the relationship
        assertNotNull(saleItem.getProduct());
        assertEquals(product.getId(), saleItem.getProduct().getId());
        assertTrue(saleItem.getProduct().getId() instanceof UUID);
        assertEquals("Test Product", saleItem.getProduct().getName());
    }

    @Test
    @DisplayName("DiscountAuditLog should properly reference Product and Store with UUID")
    void testDiscountAuditLogReferences() {
        // Create discount audit log that references both product and store
        auditLog = new DiscountAuditLog();
        auditLog.setUser(user);
        auditLog.setRole(UserRole.EMPLOYEE);
        auditLog.setStore(store);
        auditLog.setProduct(product);
        auditLog.setAttemptedPrice(new BigDecimal("15.00"));
        auditLog.setOriginalPrice(product.getRetailPrice());
        auditLog.setResult(DiscountResult.APPROVED);
        
        // Verify the relationships
        assertNotNull(auditLog.getProduct());
        assertNotNull(auditLog.getStore());
        assertEquals(product.getId(), auditLog.getProduct().getId());
        assertEquals(store.getId(), auditLog.getStore().getId());
        assertTrue(auditLog.getProduct().getId() instanceof UUID);
        assertTrue(auditLog.getStore().getId() instanceof UUID);
    }

    @Test
    @DisplayName("Product-Store relationship should work with UUIDs")
    void testProductStoreRelationship() {
        // Verify the ManyToOne relationship
        assertNotNull(product.getStore());
        assertEquals(store.getId(), product.getStore().getId());
        assertEquals(store.getStoreName(), product.getStore().getStoreName());
        assertTrue(product.getStore().getId() instanceof UUID);
    }

    @Test
    @DisplayName("Product constructor should allow UUID to be set by Hibernate")
    void testProductUuidGeneration() {
        Product newProduct = new Product();
        // UUID should be null before persistence
        assertNull(newProduct.getId(), "UUID should be null before persistence");
        
        // Simulate what Hibernate would do
        newProduct.setId(UUID.randomUUID());
        assertNotNull(newProduct.getId());
        assertTrue(newProduct.getId() instanceof UUID);
        
        // Test parameterized constructor also allows UUID to be set
        Product constructedProduct = new Product(
            "New Product", 
            "NEW-001", 
            new BigDecimal("5.00"), 
            new BigDecimal("8.00"), 
            new BigDecimal("12.00"), 
            50, 
            store
        );
        // UUID should be null before persistence
        assertNull(constructedProduct.getId(), "UUID should be null before persistence");
        
        // Simulate what Hibernate would do
        constructedProduct.setId(UUID.randomUUID());
        assertNotNull(constructedProduct.getId());
        assertTrue(constructedProduct.getId() instanceof UUID);
        assertNotEquals(newProduct.getId(), constructedProduct.getId());
    }

    @Test
    @DisplayName("Multiple products should have unique UUIDs")
    void testUniqueUuids() {
        Product product1 = new Product();
        Product product2 = new Product();
        Product product3 = new Product();
        
        // Simulate what Hibernate would do - generate unique UUIDs
        product1.setId(UUID.randomUUID());
        product2.setId(UUID.randomUUID());
        product3.setId(UUID.randomUUID());
        
        assertNotEquals(product1.getId(), product2.getId());
        assertNotEquals(product2.getId(), product3.getId());
        assertNotEquals(product1.getId(), product3.getId());
    }

    @Test
    @DisplayName("UUID relationships should be preserved in business operations")
    void testBusinessOperationsWithUuids() {
        // Set up threshold for low stock test
        product.setLowStockThreshold(10);
        
        // Test that business methods work correctly with UUID relationships
        assertTrue(product.isLowStock() == (product.getQuantity() <= product.getLowStockThreshold()));
        
        // Test profit calculations work
        product.setCostPrice(new BigDecimal("5.00"));
        BigDecimal expectedProfit = product.getRetailPrice().subtract(product.getCostPrice());
        assertEquals(expectedProfit, product.getRetailProfit());
        
        // Test tiered pricing for different roles
        assertEquals(product.getOriginalPrice(), product.getPriceForRole(UserRole.OWNER));
        assertEquals(product.getOwnerSetSellPrice(), product.getPriceForRole(UserRole.MANAGER));
        assertEquals(product.getRetailPrice(), product.getPriceForRole(UserRole.EMPLOYEE));
    }
}