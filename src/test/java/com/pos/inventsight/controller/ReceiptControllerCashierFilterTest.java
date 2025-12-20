package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class to verify cashierId parameter filtering in ReceiptController
 */
@ExtendWith(MockitoExtension.class)
public class ReceiptControllerCashierFilterTest {

    @Mock
    private SaleService saleService;

    @InjectMocks
    private ReceiptController receiptController;

    @Mock
    private Authentication authentication;

    /**
     * Test that getAllReceipts calls getAllSales when cashierId is null (All Cashiers)
     */
    @Test
    public void testGetAllReceipts_WithoutCashierId_CallsGetAllSales() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        
        List<Sale> sales = new ArrayList<>();
        Page<Sale> salesPage = new PageImpl<>(sales, PageRequest.of(0, 10), 0);
        when(saleService.getAllSales(any(Pageable.class))).thenReturn(salesPage);

        // When
        ResponseEntity<?> response = receiptController.getAllReceipts(
            0, 10, "createdAt", "desc", null, authentication
        );

        // Then
        verify(saleService, times(1)).getAllSales(any(Pageable.class));
        verify(saleService, never()).getSalesByCashier(any(UUID.class), any(Pageable.class));
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * Test that getAllReceipts calls getSalesByCashier when cashierId is provided
     */
    @Test
    public void testGetAllReceipts_WithCashierId_CallsGetSalesByCashier() {
        // Given
        UUID cashierId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("testuser");
        
        List<Sale> sales = new ArrayList<>();
        Page<Sale> salesPage = new PageImpl<>(sales, PageRequest.of(0, 10), 0);
        when(saleService.getSalesByCashier(eq(cashierId), any(Pageable.class))).thenReturn(salesPage);

        // When
        ResponseEntity<?> response = receiptController.getAllReceipts(
            0, 10, "createdAt", "desc", cashierId, authentication
        );

        // Then
        verify(saleService, times(1)).getSalesByCashier(eq(cashierId), any(Pageable.class));
        verify(saleService, never()).getAllSales(any(Pageable.class));
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * Test that the response contains the correct pagination info
     */
    @Test
    public void testGetAllReceipts_ResponseContainsPaginationInfo() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        
        List<Sale> sales = new ArrayList<>();
        Page<Sale> salesPage = new PageImpl<>(sales, PageRequest.of(0, 10), 5);
        when(saleService.getAllSales(any(Pageable.class))).thenReturn(salesPage);

        // When
        ResponseEntity<?> response = receiptController.getAllReceipts(
            0, 10, "createdAt", "desc", null, authentication
        );

        // Then
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(0, body.get("currentPage"));
        assertEquals(5L, body.get("totalItems"));
        assertEquals(1, body.get("totalPages"));
        assertEquals(10, body.get("pageSize"));
    }
}
