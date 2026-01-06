package com.pos.inventsight.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StoreInventoryBatchAddResponse DTO
 */
class StoreInventoryBatchAddResponseTest {

    @Test
    @DisplayName("Should initialize with empty lists")
    void testDefaultConstructor() {
        // Act
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        
        // Assert
        assertNotNull(response.getAdditions(), "Additions list should be initialized");
        assertNotNull(response.getErrors(), "Errors list should be initialized");
        assertTrue(response.getAdditions().isEmpty(), "Additions list should be empty");
        assertTrue(response.getErrors().isEmpty(), "Errors list should be empty");
        assertEquals(0, response.getTotalItems(), "Total items should be 0");
        assertEquals(0, response.getSuccessfulItems(), "Successful items should be 0");
        assertEquals(0, response.getFailedItems(), "Failed items should be 0");
    }

    @Test
    @DisplayName("Should track successful and failed items correctly")
    void testItemCounts() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        response.setTotalItems(5);
        response.setSuccessfulItems(3);
        response.setFailedItems(2);
        
        // Assert
        assertEquals(5, response.getTotalItems(), "Total items should be 5");
        assertEquals(3, response.getSuccessfulItems(), "Successful items should be 3");
        assertEquals(2, response.getFailedItems(), "Failed items should be 2");
    }

    @Test
    @DisplayName("Should store additions correctly")
    void testAdditions() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        List<StoreInventoryAdditionResponse> additions = new ArrayList<>();
        
        // Add mock additions (we can't create full StoreInventoryAdditionResponse without entity)
        // but we can test the setter/getter
        response.setAdditions(additions);
        
        // Assert
        assertNotNull(response.getAdditions(), "Additions should not be null");
        assertEquals(additions, response.getAdditions(), "Additions should match set value");
    }

    @Test
    @DisplayName("Should store errors correctly")
    void testErrors() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        
        StoreInventoryBatchAddResponse.BatchError error1 = 
            new StoreInventoryBatchAddResponse.BatchError(
                "product-id-1", "Product A", "Product not found"
            );
        StoreInventoryBatchAddResponse.BatchError error2 = 
            new StoreInventoryBatchAddResponse.BatchError(
                "product-id-2", "Product B", "Insufficient stock"
            );
        
        response.getErrors().add(error1);
        response.getErrors().add(error2);
        
        // Assert
        assertEquals(2, response.getErrors().size(), "Should have 2 errors");
        assertEquals("product-id-1", response.getErrors().get(0).getProductId());
        assertEquals("Product A", response.getErrors().get(0).getProductName());
        assertEquals("Product not found", response.getErrors().get(0).getError());
        assertEquals("product-id-2", response.getErrors().get(1).getProductId());
        assertEquals("Product B", response.getErrors().get(1).getProductName());
        assertEquals("Insufficient stock", response.getErrors().get(1).getError());
    }

    @Test
    @DisplayName("BatchError should initialize correctly")
    void testBatchErrorConstructor() {
        // Act
        StoreInventoryBatchAddResponse.BatchError error = 
            new StoreInventoryBatchAddResponse.BatchError(
                "test-product-id", 
                "Test Product", 
                "Test error message"
            );
        
        // Assert
        assertEquals("test-product-id", error.getProductId(), "Product ID should match");
        assertEquals("Test Product", error.getProductName(), "Product name should match");
        assertEquals("Test error message", error.getError(), "Error message should match");
    }

    @Test
    @DisplayName("BatchError setters should work correctly")
    void testBatchErrorSetters() {
        // Arrange
        StoreInventoryBatchAddResponse.BatchError error = 
            new StoreInventoryBatchAddResponse.BatchError("id1", "name1", "error1");
        
        // Act
        error.setProductId("id2");
        error.setProductName("name2");
        error.setError("error2");
        
        // Assert
        assertEquals("id2", error.getProductId(), "Product ID should be updated");
        assertEquals("name2", error.getProductName(), "Product name should be updated");
        assertEquals("error2", error.getError(), "Error message should be updated");
    }

    @Test
    @DisplayName("Should handle all successful scenario")
    void testAllSuccessful() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        response.setTotalItems(3);
        response.setSuccessfulItems(3);
        response.setFailedItems(0);
        
        // Assert
        assertEquals(3, response.getTotalItems());
        assertEquals(3, response.getSuccessfulItems());
        assertEquals(0, response.getFailedItems());
        assertTrue(response.getErrors().isEmpty(), "Should have no errors");
    }

    @Test
    @DisplayName("Should handle all failed scenario")
    void testAllFailed() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        response.setTotalItems(3);
        response.setSuccessfulItems(0);
        response.setFailedItems(3);
        
        response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
            "id1", "Product 1", "Error 1"));
        response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
            "id2", "Product 2", "Error 2"));
        response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
            "id3", "Product 3", "Error 3"));
        
        // Assert
        assertEquals(3, response.getTotalItems());
        assertEquals(0, response.getSuccessfulItems());
        assertEquals(3, response.getFailedItems());
        assertEquals(3, response.getErrors().size(), "Should have 3 errors");
        assertTrue(response.getAdditions().isEmpty(), "Should have no additions");
    }

    @Test
    @DisplayName("Should handle partial success scenario")
    void testPartialSuccess() {
        // Arrange
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        response.setTotalItems(5);
        response.setSuccessfulItems(3);
        response.setFailedItems(2);
        
        response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
            "id1", "Failed Product 1", "Error 1"));
        response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
            "id2", "Failed Product 2", "Error 2"));
        
        // Assert
        assertEquals(5, response.getTotalItems());
        assertEquals(3, response.getSuccessfulItems());
        assertEquals(2, response.getFailedItems());
        assertEquals(2, response.getErrors().size(), "Should have 2 errors");
        assertEquals(response.getSuccessfulItems() + response.getFailedItems(), 
            response.getTotalItems(), "Success + Failed should equal Total");
    }
}
