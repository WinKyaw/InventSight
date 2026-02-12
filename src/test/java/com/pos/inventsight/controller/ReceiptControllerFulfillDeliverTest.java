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
        saleResponse.setStatus(SaleStatus.COMPLETED);
        saleResponse.setFulfilledByUserId(userId);
        saleResponse.setFulfilledByUsername(username);
        saleResponse.setFulfilledAt(LocalDateTime.now());
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.COMPLETED, responseBody.getStatus());
        assertNotNull(responseBody.getFulfilledAt());
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(userId));
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
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, userId))
            .thenThrow(new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        
        // When
        ResponseEntity<?> response = receiptController.fulfillReceipt(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Receipt not found"));
        
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(userId));
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
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.fulfillReceipt(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        receiptController.fulfillReceipt(receiptId, authentication);
        
        // Then
        verify(userService, times(1)).getUserByUsername(eq(username));
        verify(saleService, times(1)).fulfillReceipt(eq(receiptId), eq(userId));
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
}
