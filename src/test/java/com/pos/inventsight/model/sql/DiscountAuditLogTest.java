package com.pos.inventsight.model.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DiscountAuditLogTest {
    
    private User user;
    private Store store;
    private Product product;
    private DiscountAuditLog auditLog;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        store = new Store();
        store.setId(1L);
        store.setStoreName("Test Store");
        
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setRetailPrice(new BigDecimal("20.00"));
        
        auditLog = new DiscountAuditLog(
            user,
            UserRole.EMPLOYEE,
            store,
            product,
            new BigDecimal("15.00"), // attempted price
            new BigDecimal("20.00"), // original price
            DiscountResult.APPROVED
        );
    }
    
    @Test
    void testDiscountAuditLogCreation() {
        assertNotNull(auditLog);
        assertEquals(user, auditLog.getUser());
        assertEquals(UserRole.EMPLOYEE, auditLog.getRole());
        assertEquals(store, auditLog.getStore());
        assertEquals(product, auditLog.getProduct());
        assertEquals(new BigDecimal("15.00"), auditLog.getAttemptedPrice());
        assertEquals(new BigDecimal("20.00"), auditLog.getOriginalPrice());
        assertEquals(DiscountResult.APPROVED, auditLog.getResult());
        assertNotNull(auditLog.getTimestamp());
    }
    
    @Test
    void testGetDiscountAmount() {
        BigDecimal discountAmount = auditLog.getDiscountAmount();
        assertEquals(new BigDecimal("5.00"), discountAmount); // 20.00 - 15.00
    }
    
    @Test
    void testGetDiscountPercentage() {
        BigDecimal discountPercentage = auditLog.getDiscountPercentage();
        assertEquals(new BigDecimal("25.0000"), discountPercentage); // 5.00 / 20.00 * 100
    }
    
    @Test
    void testGetDiscountPercentageWithZeroOriginalPrice() {
        DiscountAuditLog zeroAuditLog = new DiscountAuditLog(
            user,
            UserRole.EMPLOYEE,
            store,
            product,
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            DiscountResult.DENIED
        );
        
        BigDecimal discountPercentage = zeroAuditLog.getDiscountPercentage();
        assertEquals(BigDecimal.ZERO, discountPercentage);
    }
    
    @Test
    void testIsApproved() {
        assertTrue(auditLog.isApproved());
        
        DiscountAuditLog deniedLog = new DiscountAuditLog(
            user,
            UserRole.EMPLOYEE,
            store,
            product,
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            DiscountResult.DENIED
        );
        
        assertFalse(deniedLog.isApproved());
    }
    
    @Test
    void testDiscountResultEnum() {
        assertEquals("APPROVED", DiscountResult.APPROVED.name());
        assertEquals("DENIED", DiscountResult.DENIED.name());
        assertEquals("PENDING_APPROVAL", DiscountResult.PENDING_APPROVAL.name());
        assertEquals("AUTO_APPROVED", DiscountResult.AUTO_APPROVED.name());
        assertEquals("EXPIRED", DiscountResult.EXPIRED.name());
    }
    
    @Test
    void testSettersAndGetters() {
        auditLog.setReason("Manager override");
        auditLog.setApprovedBy("manager1");
        auditLog.setSessionId("session-123");
        
        assertEquals("Manager override", auditLog.getReason());
        assertEquals("manager1", auditLog.getApprovedBy());
        assertEquals("session-123", auditLog.getSessionId());
    }
    
    @Test
    void testNoDiscountScenario() {
        DiscountAuditLog noDiscountLog = new DiscountAuditLog(
            user,
            UserRole.CUSTOMER,
            store,
            product,
            new BigDecimal("20.00"), // same as original
            new BigDecimal("20.00"),
            DiscountResult.AUTO_APPROVED
        );
        
        assertEquals(new BigDecimal("0.00"), noDiscountLog.getDiscountAmount());
        assertEquals(new BigDecimal("0.0000"), noDiscountLog.getDiscountPercentage());
    }
}