package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the search endpoint uses "query" parameter correctly
 */
public class ProductControllerSearchTest {

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
    
    private Product createTestProduct(String name, String sku) {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setQuantity(10);
        product.setPrice(new BigDecimal("100.00"));
        product.setIsActive(true);
        return product;
    }

    /**
     * Test that the search endpoint accepts "query" parameter and returns results
     */
    @Test
    void testSearchProducts_WithQueryParameter_ReturnsResults() {
        // Given: A search query "apple"
        String searchQuery = "apple";
        int page = 0;
        int size = 20;
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Create mock products with required fields
        List<Product> products = new ArrayList<>();
        products.add(createTestProduct("Apple iPhone", "APPLE-001"));
        products.add(createTestProduct("Apple Watch", "APPLE-002"));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());
        
        // Mock service call
        when(productService.searchProducts(eq(searchQuery), any(Pageable.class)))
            .thenReturn(productPage);
        
        // When: Calling searchProducts with "query" parameter
        ResponseEntity<?> response = controller.searchProducts(searchQuery, page, size, authentication);
        
        // Then: Should return successful response
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return 200 OK");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        
        // Verify response contains search metadata
        assertEquals(searchQuery, responseBody.get("query"), "Response should contain the search query");
        assertEquals(0, responseBody.get("currentPage"), "Should return correct page number");
        assertEquals(2L, responseBody.get("totalItems"), "Should return correct total items");
        assertEquals(1, responseBody.get("totalPages"), "Should return correct total pages");
        assertEquals(20, responseBody.get("pageSize"), "Should return correct page size");
        assertEquals("InventSight", responseBody.get("system"), "Should return system name");
        
        // Verify products are in response
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> returnedProducts = (List<Map<String, Object>>) responseBody.get("products");
        assertNotNull(returnedProducts, "Products should not be null");
        
        // Verify service was called with correct parameters
        verify(productService).searchProducts(eq(searchQuery), any(Pageable.class));
    }

    /**
     * Test that search works with pagination
     */
    @Test
    void testSearchProducts_WithPagination_ReturnsPaginatedResults() {
        // Given: A search query with page 1, size 10
        String searchQuery = "test";
        int page = 1;
        int size = 10;
        
        when(authentication.getName()).thenReturn("testuser");
        
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            products.add(createTestProduct("Test Product " + i, "TEST-00" + i));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = new PageImpl<>(products, pageable, 25); // Total 25 items
        
        when(productService.searchProducts(eq(searchQuery), any(Pageable.class)))
            .thenReturn(productPage);
        
        // When
        ResponseEntity<?> response = controller.searchProducts(searchQuery, page, size, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals(searchQuery, responseBody.get("query"));
        assertEquals(1, responseBody.get("currentPage"), "Should return page 1");
        assertEquals(25L, responseBody.get("totalItems"), "Should return total items");
        assertEquals(3, responseBody.get("totalPages"), "Should return 3 total pages (25 items / 10 per page)");
        assertEquals(10, responseBody.get("pageSize"));
    }

    /**
     * Test that search handles empty results correctly
     */
    @Test
    void testSearchProducts_WithNoResults_ReturnsEmptyList() {
        // Given: A search query that returns no results
        String searchQuery = "nonexistent";
        int page = 0;
        int size = 20;
        
        when(authentication.getName()).thenReturn("testuser");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        
        when(productService.searchProducts(eq(searchQuery), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        // When
        ResponseEntity<?> response = controller.searchProducts(searchQuery, page, size, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        assertEquals(searchQuery, responseBody.get("query"));
        assertEquals(0L, responseBody.get("totalItems"), "Should return 0 items");
        assertEquals(0, responseBody.get("totalPages"), "Should return 0 pages");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> returnedProducts = (List<Map<String, Object>>) responseBody.get("products");
        assertTrue(returnedProducts.isEmpty(), "Products list should be empty");
    }
}
