package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.PredefinedItem;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify warehouse products API filtering
 * Ensures that the endpoint returns only products assigned to the specific warehouse
 */
public class WarehouseControllerAvailableProductsTest {

    private WarehouseController controller;
    private WarehouseRepository warehouseRepository;
    private ProductRepository productRepository;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new WarehouseController();
        warehouseRepository = mock(WarehouseRepository.class);
        productRepository = mock(ProductRepository.class);
        authentication = mock(Authentication.class);
        
        // Inject mocks using reflection
        ReflectionTestUtils.setField(controller, "warehouseRepository", warehouseRepository);
        ReflectionTestUtils.setField(controller, "productRepository", productRepository);
        
        // Setup authentication
        when(authentication.getName()).thenReturn("testuser");
    }

    /**
     * Test that API returns empty array for warehouse with no products
     */
    @Test
    void testGetWarehouseAvailableProducts_EmptyWarehouse_ReturnsEmptyList() {
        // Given: Warehouse with no products assigned
        UUID warehouseId = UUID.fromString("8e05f2cb-dd12-4ce1-8a17-a71e6d5ea901");
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("၆ ခမ်း");
        warehouse.setLocation("Test Location");
        
        when(warehouseRepository.findById(warehouseId))
            .thenReturn(Optional.of(warehouse));
        when(productRepository.findByWarehouseId(warehouseId))
            .thenReturn(Collections.emptyList());
        
        // When
        ResponseEntity<?> response = controller.getWarehouseAvailableProducts(
            warehouseId, authentication
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        
        assertTrue((Boolean) body.get("success"));
        assertEquals(warehouseId, body.get("warehouseId"));
        assertEquals("၆ ခမ်း", body.get("warehouseName"));
        assertEquals(0, body.get("count"));
        
        @SuppressWarnings("unchecked")
        List<?> products = (List<?>) body.get("products");
        assertTrue(products.isEmpty());
        
        // Verify repository methods were called
        verify(warehouseRepository).findById(warehouseId);
        verify(productRepository).findByWarehouseId(warehouseId);
    }

    /**
     * Test that API returns only products assigned to specific warehouse
     */
    @Test
    void testGetWarehouseAvailableProducts_WithProducts_ReturnsFilteredList() {
        // Given: Warehouse with 4 assigned products
        UUID warehouseId = UUID.fromString("d48523cb-e888-4596-832a-b8174846bcc5");
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("၁၈လမ်း");
        warehouse.setLocation("Test Location");
        
        List<Product> expectedProducts = Arrays.asList(
            createProduct(UUID.randomUUID(), "Apples", "61078664613", "food", "lb", 
                new BigDecimal("2.99"), UUID.fromString("954ab8e5-71d6-48e7-ae5f-12d7fed79a14")),
            createProduct(UUID.randomUUID(), "Ttt", "99467823160", "misc", "piece", 
                new BigDecimal("5.00"), UUID.randomUUID()),
            createProduct(UUID.randomUUID(), "Ty", "67343576355", "misc", "piece", 
                new BigDecimal("10.00"), UUID.randomUUID()),
            createProduct(UUID.randomUUID(), "နဂါးကြီး", "55231605595", "misc", "piece", 
                new BigDecimal("15.00"), UUID.randomUUID())
        );
        
        when(warehouseRepository.findById(warehouseId))
            .thenReturn(Optional.of(warehouse));
        when(productRepository.findByWarehouseId(warehouseId))
            .thenReturn(expectedProducts);
        
        // When
        ResponseEntity<?> response = controller.getWarehouseAvailableProducts(
            warehouseId, authentication
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        
        assertTrue((Boolean) body.get("success"));
        assertEquals(warehouseId, body.get("warehouseId"));
        assertEquals("၁၈လမ်း", body.get("warehouseName"));
        assertEquals(4, body.get("count"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("products");
        assertEquals(4, products.size());
        
        // Verify first product details
        Map<String, Object> firstProduct = products.get(0);
        assertEquals("Apples", firstProduct.get("name"));
        assertEquals("61078664613", firstProduct.get("sku"));
        assertEquals("food", firstProduct.get("category"));
        assertEquals("lb", firstProduct.get("unitType"));
        assertEquals(new BigDecimal("2.99"), firstProduct.get("price"));
        assertEquals(warehouseId, firstProduct.get("warehouseId")); // ✅ Verify warehouseId is included
        assertEquals(UUID.fromString("954ab8e5-71d6-48e7-ae5f-12d7fed79a14"), 
                     firstProduct.get("predefinedItemId"));
        
        // Verify repository methods were called
        verify(warehouseRepository).findById(warehouseId);
        verify(productRepository).findByWarehouseId(warehouseId);
    }

    /**
     * Test error handling for non-existent warehouse
     */
    @Test
    void testGetWarehouseAvailableProducts_NonExistentWarehouse_ReturnsNotFound() {
        // Given: Non-existent warehouse ID
        UUID warehouseId = UUID.randomUUID();
        
        when(warehouseRepository.findById(warehouseId))
            .thenReturn(Optional.empty());
        
        // When
        ResponseEntity<?> response = controller.getWarehouseAvailableProducts(
            warehouseId, authentication
        );
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("error").toString().contains("Warehouse not found"));
        
        // Verify repository was called but products were never queried
        verify(warehouseRepository).findById(warehouseId);
        verify(productRepository, never()).findByWarehouseId(any());
    }

    /**
     * Test that each product includes the warehouseId field
     */
    @Test
    void testGetWarehouseAvailableProducts_ProductsIncludeWarehouseId() {
        // Given: Warehouse with one product
        UUID warehouseId = UUID.randomUUID();
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Test Warehouse");
        
        Product product = createProduct(UUID.randomUUID(), "Test Product", "12345", 
            "test", "piece", new BigDecimal("1.00"), UUID.randomUUID());
        
        when(warehouseRepository.findById(warehouseId))
            .thenReturn(Optional.of(warehouse));
        when(productRepository.findByWarehouseId(warehouseId))
            .thenReturn(Collections.singletonList(product));
        
        // When
        ResponseEntity<?> response = controller.getWarehouseAvailableProducts(
            warehouseId, authentication
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("products");
        
        assertEquals(1, products.size());
        
        Map<String, Object> productMap = products.get(0);
        
        // ✅ Verify all expected fields are present
        assertTrue(productMap.containsKey("id"));
        assertTrue(productMap.containsKey("name"));
        assertTrue(productMap.containsKey("sku"));
        assertTrue(productMap.containsKey("description"));
        assertTrue(productMap.containsKey("category"));
        assertTrue(productMap.containsKey("unitType"));
        assertTrue(productMap.containsKey("price"));
        assertTrue(productMap.containsKey("warehouseId")); // ✅ Critical field
        assertTrue(productMap.containsKey("predefinedItemId"));
        
        // Verify warehouseId matches the request
        assertEquals(warehouseId, productMap.get("warehouseId"));
    }

    /**
     * Helper method to create a test product
     */
    private Product createProduct(UUID id, String name, String sku, String category, 
                                 String unit, BigDecimal price, UUID predefinedItemId) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        product.setDescription("Test description for " + name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setRetailPrice(price);
        product.setIsActive(true);
        
        if (predefinedItemId != null) {
            PredefinedItem predefinedItem = new PredefinedItem();
            predefinedItem.setId(predefinedItemId);
            product.setPredefinedItem(predefinedItem);
        }
        
        return product;
    }
}
