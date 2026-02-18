package com.pos.inventsight.controller;

import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.SaleService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the complete payment status workflow.
 * 
 * Expected Flow:
 * 1. Create Receipt → PENDING
 * 2. Pay Now → PAID ✅
 * 3. Fulfill → READY_FOR_PICKUP/OUT_FOR_DELIVERY
 * 4. Complete → COMPLETED ✅
 */
@ExtendWith(MockitoExtension.class)
public class PaymentStatusWorkflowTest {

    @Mock
    private SaleService saleService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReceiptController receiptController;

    @Mock
    private Authentication authentication;

    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        mockUser.setRole(UserRole.CASHIER);
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);
    }

    /**
     * Test that payment changes status from PENDING to PAID (not COMPLETED)
     */
    @Test
    public void testPayment_ChangesPendingToPaid() {
        Long receiptId = 1L;
        Map<String, Object> request = new HashMap<>();
        request.put("paymentMethod", "CASH");
        
        SaleResponse paidResponse = new SaleResponse();
        paidResponse.setId(receiptId);
        paidResponse.setStatus(SaleStatus.PAID);  // Should be PAID, not COMPLETED
        paidResponse.setPaymentMethod(PaymentMethod.CASH);
        paidResponse.setReceiptNumber("INV-001");
        
        when(saleService.completeReceipt(eq(receiptId), eq(PaymentMethod.CASH), eq(mockUser.getId())))
            .thenReturn(paidResponse);
        
        ResponseEntity<?> response = receiptController.completeReceipt(receiptId, request, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SaleResponse result = (SaleResponse) response.getBody();
        
        // ✅ CRITICAL: Status after payment should be PAID, not COMPLETED
        assertEquals(SaleStatus.PAID, result.getStatus(), 
            "After payment, status should be PAID, not COMPLETED");
        assertNotNull(result.getPaymentMethod());
    }

    /**
     * Test that PICKUP fulfillment changes PAID to READY_FOR_PICKUP
     */
    @Test
    public void testFulfillment_PickupReceipt_ChangesPaidToReadyForPickup() {
        Long receiptId = 2L;
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "PICKUP");
        
        SaleResponse fulfilledResponse = new SaleResponse();
        fulfilledResponse.setId(receiptId);
        fulfilledResponse.setStatus(SaleStatus.READY_FOR_PICKUP);
        fulfilledResponse.setPaymentMethod(PaymentMethod.CASH);
        fulfilledResponse.setReceiptType(ReceiptType.PICKUP);
        
        when(saleService.fulfillReceipt(eq(receiptId), eq(ReceiptType.PICKUP), eq(mockUser.getId())))
            .thenReturn(fulfilledResponse);
        
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SaleResponse result = (SaleResponse) response.getBody();
        
        // Status should change from PAID to READY_FOR_PICKUP
        assertEquals(SaleStatus.READY_FOR_PICKUP, result.getStatus());
        assertEquals(ReceiptType.PICKUP, result.getReceiptType());
    }

    /**
     * Test that DELIVERY fulfillment changes PAID to OUT_FOR_DELIVERY
     */
    @Test
    public void testFulfillment_DeliveryReceipt_ChangesPaidToOutForDelivery() {
        Long receiptId = 3L;
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "DELIVERY");
        
        SaleResponse fulfilledResponse = new SaleResponse();
        fulfilledResponse.setId(receiptId);
        fulfilledResponse.setStatus(SaleStatus.OUT_FOR_DELIVERY);
        fulfilledResponse.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        fulfilledResponse.setReceiptType(ReceiptType.DELIVERY);
        
        when(saleService.fulfillReceipt(eq(receiptId), eq(ReceiptType.DELIVERY), eq(mockUser.getId())))
            .thenReturn(fulfilledResponse);
        
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SaleResponse result = (SaleResponse) response.getBody();
        
        // Status should change from PAID to OUT_FOR_DELIVERY
        assertEquals(SaleStatus.OUT_FOR_DELIVERY, result.getStatus());
        assertEquals(ReceiptType.DELIVERY, result.getReceiptType());
    }

    /**
     * Test that fulfillment without payment fails
     */
    @Test
    public void testFulfillment_RequiresPaidStatus() {
        Long receiptId = 4L;
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "PICKUP");
        
        when(saleService.fulfillReceipt(eq(receiptId), eq(ReceiptType.PICKUP), eq(mockUser.getId())))
            .thenThrow(new IllegalStateException("Receipt must be paid before fulfillment. Current status: PENDING"));
        
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Should return BAD_REQUEST when trying to fulfill unpaid receipt
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Test IN_STORE receipt type goes to COMPLETED after fulfillment
     */
    @Test
    public void testFulfillment_InStoreReceipt_ChangesToCompleted() {
        Long receiptId = 5L;
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "IN_STORE");
        
        SaleResponse fulfilledResponse = new SaleResponse();
        fulfilledResponse.setId(receiptId);
        fulfilledResponse.setStatus(SaleStatus.COMPLETED);
        fulfilledResponse.setPaymentMethod(PaymentMethod.CASH);
        fulfilledResponse.setReceiptType(ReceiptType.IN_STORE);
        
        when(saleService.fulfillReceipt(eq(receiptId), eq(ReceiptType.IN_STORE), eq(mockUser.getId())))
            .thenReturn(fulfilledResponse);
        
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SaleResponse result = (SaleResponse) response.getBody();
        
        // IN_STORE fulfillment should go directly to COMPLETED
        assertEquals(SaleStatus.COMPLETED, result.getStatus());
        assertEquals(ReceiptType.IN_STORE, result.getReceiptType());
    }
}
