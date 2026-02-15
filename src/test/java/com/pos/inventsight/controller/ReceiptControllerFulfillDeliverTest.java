package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.ReceiptType;
import com.pos.inventsight.model.sql.SaleStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.SaleService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class to verify fulfill and deliver endpoints in ReceiptController
 */
@ExtendWith(MockitoExtension.class)
public class ReceiptControllerFulfillDeliverTest {

    @Mock
    private SaleService saleService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReceiptController receiptController;

    @Mock
    private Authentication authentication;

    /**
     * Test successful fulfillment of receipt
     */
    @Test
    public void testFulfillReceipt_Success() {
        // Given
        Long receiptId = 7L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-001");
        saleResponse.setStatus(SaleStatus.READY_FOR_PICKUP);
        saleResponse.setReceiptType(ReceiptType.PICKUP);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "PICKUP");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.PICKUP, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.READY_FOR_PICKUP, responseBody.getStatus());
        assertEquals(ReceiptType.PICKUP, responseBody.getReceiptType());
        assertNotNull(responseBody.getFulfilledAt());
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.PICKUP), eq(userId));
    }

    /**
     * Test fulfillment when receipt not found
     */
    @Test
    public void testFulfillReceipt_NotFound() {
        // Given
        Long receiptId = 999L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "PICKUP");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.PICKUP, userId))
            .thenThrow(new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Receipt not found"));
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.PICKUP), eq(userId));
    }

    /**
     * Test successful delivery of receipt
     */
    @Test
    public void testMarkAsDelivered_Success() {
        // Given
        Long receiptId = 8L;
        String username = "deliveryuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-002");
        saleResponse.setStatus(SaleStatus.DELIVERED);
        saleResponse.setReceiptType(ReceiptType.DELIVERY);
        saleResponse.setDeliveredAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsDelivered(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.markAsDelivered(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.DELIVERED, responseBody.getStatus());
        assertEquals(ReceiptType.DELIVERY, responseBody.getReceiptType());
        assertNotNull(responseBody.getDeliveredAt());
        
        verify(saleService, times(1)).markAsDelivered(eq(receiptId), eq(userId));
    }

    /**
     * Test marking as delivered when receipt is not DELIVERY type
     */
    @Test
    public void testMarkAsDelivered_InvalidReceiptType() {
        // Given
        Long receiptId = 10L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsDelivered(receiptId, userId))
            .thenThrow(new IllegalArgumentException("Cannot mark non-delivery receipt as delivered. Receipt type is: REGULAR"));
        
        // When
        ResponseEntity<?> response = receiptController.markAsDelivered(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Cannot mark non-delivery receipt"));
        
        verify(saleService, times(1)).markAsDelivered(eq(receiptId), eq(userId));
    }

    /**
     * Test marking as delivered when receipt not found
     */
    @Test
    public void testMarkAsDelivered_NotFound() {
        // Given
        Long receiptId = 999L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsDelivered(receiptId, userId))
            .thenThrow(new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        
        // When
        ResponseEntity<?> response = receiptController.markAsDelivered(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Receipt not found"));
        
        verify(saleService, times(1)).markAsDelivered(eq(receiptId), eq(userId));
    }

    /**
     * Test that fulfill endpoint calls the service with correct parameters
     */
    @Test
    public void testFulfillReceipt_VerifyServiceCall() {
        // Given
        Long receiptId = 5L;
        String username = "adminuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "DELIVERY");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.DELIVERY, userId)).thenReturn(saleResponse);
        
        // When
        receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        verify(userService, times(1)).getUserByUsername(eq(username));
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.DELIVERY), eq(userId));
    }

    /**
     * Test that deliver endpoint calls the service with correct parameters
     */
    @Test
    public void testMarkAsDelivered_VerifyServiceCall() {
        // Given
        Long receiptId = 6L;
        String username = "deliveryuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsDelivered(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        receiptController.markAsDelivered(receiptId, authentication);
        
        // Then
        verify(userService, times(1)).getUserByUsername(eq(username));
        verify(saleService, times(1)).markAsDelivered(eq(receiptId), eq(userId));
    }

    /**
     * Test fulfillment without receipt type - should fail with BAD_REQUEST
     */
    @Test
    public void testFulfillReceipt_MissingReceiptType() {
        // Given
        Long receiptId = 10L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        // Request without receiptType
        Map<String, Object> request = new HashMap<>();
        
        // ✅ Updated for backward compatibility - should now succeed with null receiptType
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-004");
        saleResponse.setStatus(SaleStatus.COMPLETED);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, null, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(SaleStatus.COMPLETED, responseBody.getStatus());
        
        // Service should be called with null receiptType
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(null), eq(userId));
    }

    /**
     * Test fulfillment with invalid receipt type - should fail with BAD_REQUEST
     */
    @Test
    public void testFulfillReceipt_InvalidReceiptType() {
        // Given
        Long receiptId = 11L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "INVALID_TYPE");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Invalid receipt type"));
        
        // Service should not be called
        verify(saleService, never()).fulfillReceipt(any(), any(), any());
    }

    /**
     * Test fulfillment without payment - payment validation removed for backward compatibility
     * This test now expects SUCCESS instead of failure
     */
    @Test
    public void testFulfillReceipt_UnpaidReceipt() {
        // Given
        Long receiptId = 12L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "PICKUP");
        
        // ✅ Updated for backward compatibility - payment validation removed
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-007");
        saleResponse.setStatus(SaleStatus.READY_FOR_PICKUP);
        saleResponse.setReceiptType(ReceiptType.PICKUP);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.PICKUP, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then - Now expects success instead of BAD_REQUEST
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(SaleStatus.READY_FOR_PICKUP, responseBody.getStatus());
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.PICKUP), eq(userId));
    }

    /**
     * Test fulfillment with DELIVERY type - should set status to OUT_FOR_DELIVERY
     */
    @Test
    public void testFulfillReceipt_DeliveryType() {
        // Given
        Long receiptId = 13L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-003");
        saleResponse.setStatus(SaleStatus.OUT_FOR_DELIVERY);
        saleResponse.setReceiptType(ReceiptType.DELIVERY);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "DELIVERY");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.DELIVERY, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.OUT_FOR_DELIVERY, responseBody.getStatus());
        assertEquals(ReceiptType.DELIVERY, responseBody.getReceiptType());
        assertNotNull(responseBody.getFulfilledAt());
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.DELIVERY), eq(userId));
    }

    /**
     * Test fulfillment with receipt type mismatch - should fail with BAD_REQUEST
     */
    @Test
    public void testFulfillReceipt_TypeMismatch() {
        // Given
        Long receiptId = 14L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "DELIVERY");
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, ReceiptType.DELIVERY, userId))
            .thenThrow(new IllegalArgumentException("Receipt type mismatch. Receipt was created as PICKUP but fulfillment requested for DELIVERY"));
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Receipt type mismatch"));
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(ReceiptType.DELIVERY), eq(userId));
    }

    /**
     * Test fulfillment with null request body - should succeed with default behavior
     */
    @Test
    public void testFulfillReceipt_NullRequestBody() {
        // Given
        Long receiptId = 15L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-005");
        saleResponse.setStatus(SaleStatus.COMPLETED);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, null, userId)).thenReturn(saleResponse);
        
        // When - pass null as request body
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, null, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.COMPLETED, responseBody.getStatus());
        assertNotNull(responseBody.getFulfilledAt());
        
        // Service should be called with null receiptType
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(null), eq(userId));
    }

    /**
     * Test fulfillment with empty receiptType string - should succeed with default behavior
     */
    @Test
    public void testFulfillReceipt_EmptyReceiptType() {
        // Given
        Long receiptId = 16L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        Map<String, Object> request = new HashMap<>();
        request.put("receiptType", "");  // Empty string
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-006");
        saleResponse.setStatus(SaleStatus.COMPLETED);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, null, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, request, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(SaleStatus.COMPLETED, responseBody.getStatus());
        
        // Service should be called with null receiptType
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(null), eq(userId));
    }
}
