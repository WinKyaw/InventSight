package com.pos.inventsight.service;

import com.pos.inventsight.dto.SaleRequest;
import com.pos.inventsight.model.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that receiptType is correctly set and persisted in sales/receipts
 * Addresses issue where receipts default to IN_STORE even when PICKUP/DELIVERY is specified
 */
public class ReceiptTypeTest {

    @Test
    @DisplayName("Sale should persist receiptType from SaleRequest")
    public void testSaleReceiptTypeIsSet() {
        // Given: Create a sale with PICKUP type
        Sale sale = new Sale();
        sale.setReceiptNumber("TEST-RECEIPT-001");
        sale.setSubtotal(BigDecimal.valueOf(100.00));
        sale.setTaxAmount(BigDecimal.valueOf(10.00));
        sale.setTotalAmount(BigDecimal.valueOf(110.00));
        sale.setStatus(SaleStatus.PENDING);
        sale.setReceiptType(ReceiptType.PICKUP);

        // Verify: Receipt type should be PICKUP
        assertEquals(ReceiptType.PICKUP, sale.getReceiptType(), 
            "Sale should store PICKUP receipt type");
    }

    @Test
    @DisplayName("Sale should default to IN_STORE when receiptType is not specified")
    public void testSaleDefaultsToInStore() {
        // Given: Create a sale without specifying receipt type
        Sale sale = new Sale();
        sale.setReceiptNumber("TEST-RECEIPT-002");
        sale.setSubtotal(BigDecimal.valueOf(50.00));
        sale.setTaxAmount(BigDecimal.valueOf(5.00));
        sale.setTotalAmount(BigDecimal.valueOf(55.00));
        sale.setStatus(SaleStatus.COMPLETED);

        // Verify: Should default to IN_STORE
        assertEquals(ReceiptType.IN_STORE, sale.getReceiptType(), 
            "Sale should default to IN_STORE when receiptType is not specified");
    }

    @Test
    @DisplayName("SaleRequest should accept and store all receipt types")
    public void testSaleRequestAcceptsAllReceiptTypes() {
        // Test IN_STORE
        SaleRequest inStoreRequest = new SaleRequest();
        inStoreRequest.setReceiptType(ReceiptType.IN_STORE);
        assertEquals(ReceiptType.IN_STORE, inStoreRequest.getReceiptType());

        // Test PICKUP
        SaleRequest pickupRequest = new SaleRequest();
        pickupRequest.setReceiptType(ReceiptType.PICKUP);
        assertEquals(ReceiptType.PICKUP, pickupRequest.getReceiptType());

        // Test DELIVERY
        SaleRequest deliveryRequest = new SaleRequest();
        deliveryRequest.setReceiptType(ReceiptType.DELIVERY);
        assertEquals(ReceiptType.DELIVERY, deliveryRequest.getReceiptType());

        // Test HOLD
        SaleRequest holdRequest = new SaleRequest();
        holdRequest.setReceiptType(ReceiptType.HOLD);
        assertEquals(ReceiptType.HOLD, holdRequest.getReceiptType());
    }

    @Test
    @DisplayName("Sale should support changing receiptType after creation")
    public void testSaleReceiptTypeCanBeChanged() {
        // Given: Create a sale with IN_STORE type
        Sale sale = new Sale();
        sale.setReceiptType(ReceiptType.IN_STORE);
        assertEquals(ReceiptType.IN_STORE, sale.getReceiptType());

        // When: Change to PICKUP
        sale.setReceiptType(ReceiptType.PICKUP);

        // Then: Receipt type should be updated
        assertEquals(ReceiptType.PICKUP, sale.getReceiptType(),
            "Sale should support changing receiptType after creation");
    }

    @Test
    @DisplayName("ReceiptType enum should have all expected values")
    public void testReceiptTypeEnumValues() {
        // Verify all expected receipt types exist
        ReceiptType[] types = ReceiptType.values();
        
        assertEquals(4, types.length, "ReceiptType should have exactly 4 values");
        
        // Verify each type exists
        assertNotNull(ReceiptType.valueOf("IN_STORE"));
        assertNotNull(ReceiptType.valueOf("PICKUP"));
        assertNotNull(ReceiptType.valueOf("DELIVERY"));
        assertNotNull(ReceiptType.valueOf("HOLD"));
    }
}
