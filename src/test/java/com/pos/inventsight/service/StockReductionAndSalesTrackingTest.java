package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class to verify that stock reduction properly updates:
 * 1. Product quantity
 * 2. Total sales counter
 * 3. Last sold date
 */
@ExtendWith(MockitoExtension.class)
public class StockReductionAndSalesTrackingTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private UUID productId;

    @BeforeEach
    public void setUp() {
        productId = UUID.randomUUID();
        
        // Create test product
        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Test Product");
        testProduct.setSku("TEST-001");
        testProduct.setQuantity(100);
        testProduct.setTotalSales(0);
        testProduct.setLastSoldDate(null);
        testProduct.setPrice(BigDecimal.valueOf(10.00));
        testProduct.setOriginalPrice(BigDecimal.valueOf(10.00));
        testProduct.setOwnerSetSellPrice(BigDecimal.valueOf(15.00));
        testProduct.setRetailPrice(BigDecimal.valueOf(20.00));
        testProduct.setLowStockThreshold(10);
    }

    /**
     * Test that reduceStock properly updates quantity, totalSales, and lastSoldDate
     */
    @Test
    public void testReduceStock_UpdatesQuantityAndSalesTracking() {
        // Given
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            // Simulate persistence
            return saved;
        });

        // When
        productService.reduceStock(productId, 10, "Test Sale");

        // Then
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        verify(productRepository, times(2)).findById(productId); // Once to get, once to verify
        
        // Verify the product was updated correctly
        assertEquals(90, testProduct.getQuantity(), "Quantity should be reduced by 10");
        assertEquals(10, testProduct.getTotalSales(), "Total sales should be incremented by 10");
        assertNotNull(testProduct.getLastSoldDate(), "Last sold date should be set");
    }

    /**
     * Test that reduceStock throws exception when insufficient stock
     */
    @Test
    public void testReduceStock_ThrowsException_WhenInsufficientStock() {
        // Given
        testProduct.setQuantity(5);
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThrows(InsufficientStockException.class, () -> {
            productService.reduceStock(productId, 10, "Test Sale");
        });
        
        // Verify no save was attempted
        verify(productRepository, never()).save(any(Product.class));
    }

    /**
     * Test that multiple stock reductions accumulate totalSales correctly
     */
    @Test
    public void testReduceStock_AccumulatesTotalSales() {
        // Given
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });

        // When - First reduction
        productService.reduceStock(productId, 5, "Sale 1");
        
        // Update the mock to return the modified product
        testProduct.setQuantity(95);
        testProduct.setTotalSales(5);
        
        // When - Second reduction
        productService.reduceStock(productId, 3, "Sale 2");
        
        testProduct.setQuantity(92);
        testProduct.setTotalSales(8);
        
        // When - Third reduction
        productService.reduceStock(productId, 2, "Sale 3");

        // Then
        assertEquals(90, testProduct.getQuantity(), "Quantity should be 100 - 5 - 3 - 2 = 90");
        assertEquals(10, testProduct.getTotalSales(), "Total sales should be 5 + 3 + 2 = 10");
    }

    /**
     * Test that totalSales starts from 0 if null
     */
    @Test
    public void testReduceStock_HandlesNullTotalSales() {
        // Given
        testProduct.setTotalSales(null); // Simulate old product without totalSales field
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });

        // When
        productService.reduceStock(productId, 5, "Test Sale");

        // Then
        assertEquals(5, testProduct.getTotalSales(), "Total sales should be set to 5 (starting from 0)");
        assertEquals(95, testProduct.getQuantity(), "Quantity should be reduced to 95");
    }

    /**
     * Test that lastSoldDate is updated on each sale
     */
    @Test
    public void testReduceStock_UpdatesLastSoldDate() {
        // Given
        LocalDateTime beforeSale = LocalDateTime.now().minusDays(1);
        testProduct.setLastSoldDate(beforeSale);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });

        // When
        productService.reduceStock(productId, 5, "Test Sale");

        // Then
        assertNotNull(testProduct.getLastSoldDate(), "Last sold date should be set");
        assertTrue(testProduct.getLastSoldDate().isAfter(beforeSale), 
            "Last sold date should be updated to a more recent time");
    }
}
