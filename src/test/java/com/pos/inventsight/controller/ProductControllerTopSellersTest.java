package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ProductResponse;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the top-sellers endpoint works correctly
 */
public class ProductControllerTopSellersTest {

    private ProductController controller;
    private ProductService productService;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new ProductController();
        productService = mock(ProductService.class);
        authentication = mock(Authentication.class);
        
        // Inject mocks using reflection
        ReflectionTestUtils.setField(controller, "productService", productService);
    }
    
    private Product createTestProduct(String name, String sku, BigDecimal retailPrice) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setSku(sku);
        product.setQuantity(10);
        product.setPrice(new BigDecimal("100.00"));
        product.setRetailPrice(retailPrice);
        product.setIsActive(true);
        product.setTotalSales(100);
        return product;
    }

    /**
     * Test that the top-sellers endpoint returns products without storeId
     */
    @Test
    void testGetTopSellers_WithoutStoreId_ReturnsResults() {
        // Given: Top selling products
        int limit = 10;
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Create mock products with retailPrice set
        List<Product> topSellers = new ArrayList<>();
        topSellers.add(createTestProduct("Best Seller 1", "BS-001", new BigDecimal("50.00")));
        topSellers.add(createTestProduct("Best Seller 2", "BS-002", new BigDecimal("75.00")));
        topSellers.add(createTestProduct("Best Seller 3", "BS-003", new BigDecimal("100.00")));
        
        // Mock service call
        when(productService.getTopSellingProducts(eq(limit)))
            .thenReturn(topSellers);
        
        // When: Calling getTopSellingProducts without storeId
        ResponseEntity<?> response = controller.getTopSellingProducts(null, limit, authentication);
        
        // Then: Should return OK status with products
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify it's a list of ProductResponse
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<ProductResponse> productResponses = (List<ProductResponse>) response.getBody();
        assertEquals(3, productResponses.size());
        
        // Verify sellingPrice is mapped from retailPrice
        assertEquals(new BigDecimal("50.00"), productResponses.get(0).getSellingPrice());
        assertEquals(new BigDecimal("75.00"), productResponses.get(1).getSellingPrice());
        assertEquals(new BigDecimal("100.00"), productResponses.get(2).getSellingPrice());
        
        // Verify service was called
        verify(productService, times(1)).getTopSellingProducts(eq(limit));
    }

    /**
     * Test that the top-sellers endpoint returns products for a specific store
     */
    @Test
    void testGetTopSellers_WithStoreId_ReturnsResults() {
        // Given: A storeId and top selling products
        UUID storeId = UUID.randomUUID();
        int limit = 5;
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Create mock products
        List<Product> topSellers = new ArrayList<>();
        topSellers.add(createTestProduct("Store Best 1", "SB-001", new BigDecimal("25.00")));
        topSellers.add(createTestProduct("Store Best 2", "SB-002", new BigDecimal("30.00")));
        
        // Mock service call
        when(productService.getTopSellingProductsByStore(eq(storeId), eq(limit)))
            .thenReturn(topSellers);
        
        // When: Calling getTopSellingProducts with storeId
        ResponseEntity<?> response = controller.getTopSellingProducts(storeId, limit, authentication);
        
        // Then: Should return OK status with products
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        List<ProductResponse> productResponses = (List<ProductResponse>) response.getBody();
        assertEquals(2, productResponses.size());
        
        // Verify sellingPrice is mapped
        assertEquals(new BigDecimal("25.00"), productResponses.get(0).getSellingPrice());
        assertEquals(new BigDecimal("30.00"), productResponses.get(1).getSellingPrice());
        
        // Verify service was called with storeId
        verify(productService, times(1)).getTopSellingProductsByStore(eq(storeId), eq(limit));
    }

    /**
     * Test that sellingPrice falls back to price when retailPrice is null
     */
    @Test
    void testGetTopSellers_WithNullRetailPrice_FallsBackToPrice() {
        // Given: A product with null retailPrice
        int limit = 1;
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Create product with null retailPrice
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Fallback Product");
        product.setSku("FB-001");
        product.setQuantity(10);
        product.setRetailPrice(null); // Null retail price (this also sets price to null)
        product.setPrice(new BigDecimal("99.99")); // Set price after retailPrice
        product.setIsActive(true);
        
        List<Product> topSellers = new ArrayList<>();
        topSellers.add(product);
        
        // Mock service call
        when(productService.getTopSellingProducts(eq(limit)))
            .thenReturn(topSellers);
        
        // When: Calling getTopSellingProducts
        ResponseEntity<?> response = controller.getTopSellingProducts(null, limit, authentication);
        
        // Then: Should return OK with sellingPrice falling back to price
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<ProductResponse> productResponses = (List<ProductResponse>) response.getBody();
        assertEquals(1, productResponses.size());
        
        // Verify sellingPrice falls back to price
        assertEquals(new BigDecimal("99.99"), productResponses.get(0).getSellingPrice());
        assertEquals(new BigDecimal("99.99"), productResponses.get(0).getPrice());
    }
}
