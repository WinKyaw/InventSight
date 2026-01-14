package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.SaleRequest;
import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.model.sql.PaymentMethod;
import com.pos.inventsight.model.sql.SaleStatus;
import com.pos.inventsight.model.sql.User;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class to verify pending receipt creation and completion functionality.
 * Tests payment method validation rules:
 * - PENDING receipts don't require payment method
 * - COMPLETED receipts require payment method
 * - Pending receipts can be completed later with payment method
 */
@ExtendWith(MockitoExtension.class)
public class PendingReceiptTest {

    @Mock
    private SaleService saleService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReceiptController receiptController;

    @Mock
    private Authentication authentication;

    private User mockUser;
    private SaleRequest pendingSaleRequest;
    private SaleRequest completedSaleRequest;
    private SaleResponse mockSaleResponse;

    @BeforeEach
    public void setUp() {
        // Set up mock user
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        
        // Set up pending sale request (no payment method)
        pendingSaleRequest = new SaleRequest();
        pendingSaleRequest.setItems(createMockItems());
        pendingSaleRequest.setStatus(SaleStatus.PENDING);
        pendingSaleRequest.setPaymentMethod(null);  // No payment for pending
        
        // Set up completed sale request (with payment method)
        completedSaleRequest = new SaleRequest();
        completedSaleRequest.setItems(createMockItems());
        completedSaleRequest.setStatus(SaleStatus.COMPLETED);
        completedSaleRequest.setPaymentMethod(PaymentMethod.CASH);
        
        // Set up mock sale response
        mockSaleResponse = new SaleResponse();
        mockSaleResponse.setId(1L);
        mockSaleResponse.setReceiptNumber("INV-12345");
        mockSaleResponse.setStatus(SaleStatus.PENDING);
        mockSaleResponse.setTotalAmount(BigDecimal.valueOf(100.00));
    }

    private ArrayList<SaleRequest.ItemRequest> createMockItems() {
        ArrayList<SaleRequest.ItemRequest> items = new ArrayList<>();
        SaleRequest.ItemRequest item = new SaleRequest.ItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(2);
        items.add(item);
        return items;
    }

    /**
     * Test that creating a PENDING receipt without payment method succeeds
     */
    @Test
    public void testCreatePendingReceipt_WithoutPaymentMethod_Succeeds() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);
        when(saleService.createSale(any(SaleRequest.class), eq(mockUser.getId())))
            .thenReturn(mockSaleResponse);

        // When
        ResponseEntity<?> response = receiptController.createReceipt(pendingSaleRequest, authentication);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse saleResponse = (SaleResponse) response.getBody();
        assertEquals("INV-12345", saleResponse.getReceiptNumber());
        
        verify(saleService, times(1)).createSale(any(SaleRequest.class), eq(mockUser.getId()));
    }

    /**
     * Test that creating a COMPLETED receipt without payment method fails
     */
    @Test
    public void testCreateCompletedReceipt_WithoutPaymentMethod_Fails() {
        // Given
        SaleRequest invalidRequest = new SaleRequest();
        invalidRequest.setItems(createMockItems());
        invalidRequest.setStatus(SaleStatus.COMPLETED);
        invalidRequest.setPaymentMethod(null);  // Missing payment method
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // When
        ResponseEntity<?> response = receiptController.createReceipt(invalidRequest, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Payment method is required"));
        
        verify(saleService, never()).createSale(any(SaleRequest.class), any(UUID.class));
    }

    /**
     * Test that creating a COMPLETED receipt with payment method succeeds
     */
    @Test
    public void testCreateCompletedReceipt_WithPaymentMethod_Succeeds() {
        // Given
        mockSaleResponse.setStatus(SaleStatus.COMPLETED);
        mockSaleResponse.setPaymentMethod(PaymentMethod.CASH);
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);
        when(saleService.createSale(any(SaleRequest.class), eq(mockUser.getId())))
            .thenReturn(mockSaleResponse);

        // When
        ResponseEntity<?> response = receiptController.createReceipt(completedSaleRequest, authentication);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse saleResponse = (SaleResponse) response.getBody();
        assertEquals(SaleStatus.COMPLETED, saleResponse.getStatus());
        assertEquals(PaymentMethod.CASH, saleResponse.getPaymentMethod());
        
        verify(saleService, times(1)).createSale(any(SaleRequest.class), eq(mockUser.getId()));
    }

    /**
     * Test that completing a pending receipt with payment method succeeds
     */
    @Test
    public void testCompleteReceipt_WithPaymentMethod_Succeeds() {
        // Given
        Long receiptId = 1L;
        Map<String, Object> request = new HashMap<>();
        request.put("paymentMethod", "CASH");
        
        SaleResponse completedResponse = new SaleResponse();
        completedResponse.setId(receiptId);
        completedResponse.setStatus(SaleStatus.COMPLETED);
        completedResponse.setPaymentMethod(PaymentMethod.CASH);
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);
        when(saleService.completeReceipt(eq(receiptId), eq(PaymentMethod.CASH), eq(mockUser.getId())))
            .thenReturn(completedResponse);

        // When
        ResponseEntity<?> response = receiptController.completeReceipt(receiptId, request, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse saleResponse = (SaleResponse) response.getBody();
        assertEquals(SaleStatus.COMPLETED, saleResponse.getStatus());
        assertEquals(PaymentMethod.CASH, saleResponse.getPaymentMethod());
        
        verify(saleService, times(1)).completeReceipt(eq(receiptId), eq(PaymentMethod.CASH), eq(mockUser.getId()));
    }

    /**
     * Test that completing a receipt without payment method fails
     */
    @Test
    public void testCompleteReceipt_WithoutPaymentMethod_Fails() {
        // Given
        Long receiptId = 1L;
        Map<String, Object> request = new HashMap<>();
        // No payment method provided
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // When
        ResponseEntity<?> response = receiptController.completeReceipt(receiptId, request, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Payment method is required"));
        
        verify(saleService, never()).completeReceipt(any(Long.class), any(PaymentMethod.class), any(UUID.class));
    }

    /**
     * Test that completing a receipt with invalid payment method fails
     */
    @Test
    public void testCompleteReceipt_WithInvalidPaymentMethod_Fails() {
        // Given
        Long receiptId = 1L;
        Map<String, Object> request = new HashMap<>();
        request.put("paymentMethod", "INVALID_METHOD");
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // When
        ResponseEntity<?> response = receiptController.completeReceipt(receiptId, request, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Invalid payment method"));
        
        verify(saleService, never()).completeReceipt(any(Long.class), any(PaymentMethod.class), any(UUID.class));
    }

    /**
     * Test that SaleRequest.requiresPaymentMethod() returns true for COMPLETED status
     */
    @Test
    public void testSaleRequest_RequiresPaymentMethod_ReturnsTrue_ForCompleted() {
        SaleRequest request = new SaleRequest();
        request.setStatus(SaleStatus.COMPLETED);
        
        assertTrue(request.requiresPaymentMethod());
    }

    /**
     * Test that SaleRequest.requiresPaymentMethod() returns false for PENDING status
     */
    @Test
    public void testSaleRequest_RequiresPaymentMethod_ReturnsFalse_ForPending() {
        SaleRequest request = new SaleRequest();
        request.setStatus(SaleStatus.PENDING);
        
        assertFalse(request.requiresPaymentMethod());
    }
}
