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
 * Test to verify the fix for stock reduction quantity mismatch issue
 * Issue: Receipt shows 3 oranges sold but stock only reduced by 2 (1000 → 998 instead of 997)
 */
@ExtendWith(MockitoExtension.class)
public class StockReductionQuantityTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProductService productService;

    private Product oranges;
    private UUID orangesId;

    @BeforeEach
    public void setUp() {
        orangesId = UUID.randomUUID();
        
        // Create Oranges product with initial stock of 1000
        oranges = new Product();
        oranges.setId(orangesId);
        oranges.setName("Oranges");
        oranges.setSku("ORANGE-001");
        oranges.setQuantity(1000);
        oranges.setTotalSales(0);
        oranges.setLastSoldDate(null);
        oranges.setPrice(BigDecimal.valueOf(1.99));
        oranges.setOriginalPrice(BigDecimal.valueOf(1.99));
        oranges.setOwnerSetSellPrice(BigDecimal.valueOf(1.99));
        oranges.setRetailPrice(BigDecimal.valueOf(1.99));
        oranges.setLowStockThreshold(10);
    }

    /**
     * Test the exact scenario from the issue: selling 3 oranges should reduce stock by 3
     * Before (Broken): Stock 1000 → 998 (reduced by 2 instead of 3)
     * After (Fixed): Stock 1000 → 997 (reduced by 3 correctly)
     */
    @Test
    public void testReduceStock_WithQuantity3_ShouldReduceBy3NotBy2() {
        // Given: Oranges with stock of 1000
        when(productRepository.findById(orangesId)).thenReturn(Optional.of(oranges));
        
        // Mock saveAndFlush to return the product with updated values
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });
        
        // Mock second findById call (for verification) to return the same product
        when(productRepository.findById(orangesId)).thenReturn(Optional.of(oranges));

        // When: Reduce stock by 3 (selling 3 oranges)
        productService.reduceStock(orangesId, 3, "SALE - Receipt: REC-001");

        // Then: Verify stock was reduced by exactly 3
        assertEquals(997, oranges.getQuantity(), 
            "Stock should be reduced by exactly 3 (1000 - 3 = 997), not by 2");
        
        // Verify total sales was incremented by 3
        assertEquals(3, oranges.getTotalSales(), 
            "Total sales should be incremented by 3");
        
        // Verify last sold date was set
        assertNotNull(oranges.getLastSoldDate(), "Last sold date should be set");
        
        // Verify saveAndFlush was called (not just save)
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        
        // Verify flush was called before verification
        verify(productRepository, times(1)).flush();
        
        // Verify product was re-fetched for verification (called twice: once to get, once to verify)
        verify(productRepository, times(2)).findById(orangesId);
    }

    /**
     * Test that quantity validation works - null quantity should throw IllegalArgumentException
     */
    @Test
    public void testReduceStock_WithNullQuantity_ShouldThrowException() {
        // When & Then - validation happens before fetching product
        assertThrows(IllegalArgumentException.class, () -> {
            productService.reduceStock(orangesId, null, "Test Sale");
        }, "Null quantity should throw IllegalArgumentException");
        
        // Verify no repository calls were made since validation failed early
        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    /**
     * Test that quantity validation works - zero quantity should throw IllegalArgumentException
     */
    @Test
    public void testReduceStock_WithZeroQuantity_ShouldThrowException() {
        // When & Then - validation happens before fetching product
        assertThrows(IllegalArgumentException.class, () -> {
            productService.reduceStock(orangesId, 0, "Test Sale");
        }, "Zero quantity should throw IllegalArgumentException");
        
        // Verify no repository calls were made since validation failed early
        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    /**
     * Test that quantity validation works - negative quantity should throw IllegalArgumentException
     */
    @Test
    public void testReduceStock_WithNegativeQuantity_ShouldThrowException() {
        // When & Then - validation happens before fetching product
        assertThrows(IllegalArgumentException.class, () -> {
            productService.reduceStock(orangesId, -5, "Test Sale");
        }, "Negative quantity should throw IllegalArgumentException");
        
        // Verify no repository calls were made since validation failed early
        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    /**
     * Test verification failure scenario
     * Simulates the bug where database doesn't persist the correct value
     */
    @Test
    public void testReduceStock_VerificationFailure_ShouldThrowException() {
        // Given: Product with stock of 1000
        when(productRepository.findById(orangesId))
            .thenReturn(Optional.of(oranges));
        
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });
        
        // Simulate verification failure: database returns different quantity
        Product verificationProduct = new Product();
        verificationProduct.setId(orangesId);
        verificationProduct.setName("Oranges");
        verificationProduct.setQuantity(998); // Bug: reduced by 2 instead of 3!
        verificationProduct.setTotalSales(3);
        
        when(productRepository.findById(orangesId))
            .thenReturn(Optional.of(oranges))  // First call: get product
            .thenReturn(Optional.of(verificationProduct));  // Second call: verification fails
        
        // When & Then: Should detect the mismatch and throw exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(orangesId, 3, "SALE - Receipt: REC-001");
        });
        
        assertTrue(exception.getMessage().contains("Stock reduction failed to persist"),
            "Exception should indicate stock reduction persistence failure");
        assertTrue(exception.getMessage().contains("Expected: 997"),
            "Exception should show expected quantity of 997");
        assertTrue(exception.getMessage().contains("Actual: 998"),
            "Exception should show actual quantity of 998");
    }

    /**
     * Test multiple sequential stock reductions to ensure quantities accumulate correctly
     */
    @Test
    public void testReduceStock_MultipleReductions_ShouldAccumulateCorrectly() {
        // Given
        when(productRepository.findById(orangesId)).thenReturn(Optional.of(oranges));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            return saved;
        });

        // First reduction: 3 oranges
        productService.reduceStock(orangesId, 3, "Sale 1");
        assertEquals(997, oranges.getQuantity(), "First reduction: 1000 - 3 = 997");
        assertEquals(3, oranges.getTotalSales(), "Total sales after first: 3");
        
        // Update mock to return modified product
        oranges.setQuantity(997);
        oranges.setTotalSales(3);
        
        // Second reduction: 5 oranges
        productService.reduceStock(orangesId, 5, "Sale 2");
        assertEquals(992, oranges.getQuantity(), "Second reduction: 997 - 5 = 992");
        assertEquals(8, oranges.getTotalSales(), "Total sales after second: 3 + 5 = 8");
        
        // Update mock again
        oranges.setQuantity(992);
        oranges.setTotalSales(8);
        
        // Third reduction: 2 oranges
        productService.reduceStock(orangesId, 2, "Sale 3");
        assertEquals(990, oranges.getQuantity(), "Third reduction: 992 - 2 = 990");
        assertEquals(10, oranges.getTotalSales(), "Total sales after third: 8 + 2 = 10");
    }
}
