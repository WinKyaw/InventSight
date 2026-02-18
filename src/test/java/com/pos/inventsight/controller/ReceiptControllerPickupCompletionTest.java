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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the pickup completion endpoint (markAsPickedUp) in ReceiptController
 */
@ExtendWith(MockitoExtension.class)
public class ReceiptControllerPickupCompletionTest {

    @Mock
    private SaleService saleService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReceiptController receiptController;

    @Mock
    private Authentication authentication;

    /**
     * Test successful pickup completion
     */
    @Test
    public void testMarkAsPickedUp_Success() {
        // Given
        Long receiptId = 100L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        saleResponse.setReceiptNumber("RCP-100");
        saleResponse.setStatus(SaleStatus.COMPLETED);
        saleResponse.setReceiptType(ReceiptType.PICKUP);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsPickedUp(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        ResponseEntity<?> response = receiptController.markAsPickedUp(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SaleResponse);
        
        SaleResponse responseBody = (SaleResponse) response.getBody();
        assertEquals(receiptId, responseBody.getId());
        assertEquals(SaleStatus.COMPLETED, responseBody.getStatus());
        assertEquals(ReceiptType.PICKUP, responseBody.getReceiptType());
        
        verify(saleService, times(1)).markAsPickedUp(eq(receiptId), eq(userId));
    }

    /**
     * Test pickup completion when receipt is not a PICKUP type
     */
    @Test
    public void testMarkAsPickedUp_NotPickupType() {
        // Given
        Long receiptId = 101L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsPickedUp(receiptId, userId))
            .thenThrow(new IllegalArgumentException("Cannot mark non-pickup receipt as picked up. Receipt type is: DELIVERY"));
        
        // When
        ResponseEntity<?> response = receiptController.markAsPickedUp(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Cannot mark non-pickup receipt"));
        
        verify(saleService, times(1)).markAsPickedUp(eq(receiptId), eq(userId));
    }

    /**
     * Test pickup completion when receipt is not in READY_FOR_PICKUP status
     */
    @Test
    public void testMarkAsPickedUp_WrongStatus() {
        // Given
        Long receiptId = 102L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsPickedUp(receiptId, userId))
            .thenThrow(new IllegalStateException("Receipt is not ready for pickup. Current status: PAID"));
        
        // When
        ResponseEntity<?> response = receiptController.markAsPickedUp(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Receipt is not ready for pickup"));
        
        verify(saleService, times(1)).markAsPickedUp(eq(receiptId), eq(userId));
    }

    /**
     * Test pickup completion when receipt not found
     */
    @Test
    public void testMarkAsPickedUp_NotFound() {
        // Given
        Long receiptId = 999L;
        String username = "testuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsPickedUp(receiptId, userId))
            .thenThrow(new ResourceNotFoundException("Receipt not found with ID: " + receiptId));
        
        // When - Note: ResourceNotFoundException is caught as generic Exception
        ResponseEntity<?> response = receiptController.markAsPickedUp(receiptId, authentication);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.getSuccess());
        assertTrue(apiResponse.getMessage().contains("Error"));
        
        verify(saleService, times(1)).markAsPickedUp(eq(receiptId), eq(userId));
    }

    /**
     * Test that pickup completion endpoint calls the service with correct parameters
     */
    @Test
    public void testMarkAsPickedUp_VerifyServiceCall() {
        // Given
        Long receiptId = 103L;
        String username = "pickupuser";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        SaleResponse saleResponse = new SaleResponse();
        saleResponse.setId(receiptId);
        
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(user);
        when(saleService.markAsPickedUp(receiptId, userId)).thenReturn(saleResponse);
        
        // When
        receiptController.markAsPickedUp(receiptId, authentication);
        
        // Then
        verify(userService, times(1)).getUserByUsername(eq(username));
        verify(saleService, times(1)).markAsPickedUp(eq(receiptId), eq(userId));
    }
}
