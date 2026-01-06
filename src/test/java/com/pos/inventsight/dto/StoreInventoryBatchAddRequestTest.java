package com.pos.inventsight.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StoreInventoryBatchAddRequest DTO
 */
class StoreInventoryBatchAddRequestTest {

    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    @Test
    @DisplayName("Should validate successfully with valid request")
    void testValidBatchRequest() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        request.setGlobalNotes("Test global notes");
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item1 = new StoreInventoryBatchAddRequest.BatchItem();
        item1.setProductId(UUID.randomUUID());
        item1.setQuantity(100);
        item1.setNotes("Item 1 notes");
        items.add(item1);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Should fail validation when storeId is null")
    void testInvalidRequestMissingStoreId() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(null);
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item = new StoreInventoryBatchAddRequest.BatchItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(50);
        items.add(item);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Store ID is required")),
                "Should have error for missing storeId");
    }

    @Test
    @DisplayName("Should fail validation when items list is empty")
    void testInvalidRequestEmptyItems() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        request.setItems(new ArrayList<>());
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one item is required")),
                "Should have error for empty items list");
    }

    @Test
    @DisplayName("Should fail validation when items list is null")
    void testInvalidRequestNullItems() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        request.setItems(null);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one item is required")),
                "Should have error for null items");
    }

    @Test
    @DisplayName("Should validate successfully with multiple items")
    void testValidRequestWithMultipleItems() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        request.setGlobalNotes("Shipment from supplier XYZ");
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        
        StoreInventoryBatchAddRequest.BatchItem item1 = new StoreInventoryBatchAddRequest.BatchItem();
        item1.setProductId(UUID.randomUUID());
        item1.setQuantity(100);
        item1.setNotes("Fresh batch");
        items.add(item1);
        
        StoreInventoryBatchAddRequest.BatchItem item2 = new StoreInventoryBatchAddRequest.BatchItem();
        item2.setProductId(UUID.randomUUID());
        item2.setQuantity(50);
        items.add(item2);
        
        StoreInventoryBatchAddRequest.BatchItem item3 = new StoreInventoryBatchAddRequest.BatchItem();
        item3.setProductId(UUID.randomUUID());
        item3.setQuantity(25);
        item3.setNotes("Damaged box - discount pricing");
        items.add(item3);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "Valid request with multiple items should have no violations");
        assertEquals(3, request.getItems().size(), "Should have 3 items");
    }

    @Test
    @DisplayName("BatchItem should fail validation when productId is null")
    void testBatchItemInvalidMissingProductId() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item = new StoreInventoryBatchAddRequest.BatchItem();
        item.setProductId(null);
        item.setQuantity(100);
        items.add(item);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Product ID is required")),
                "Should have error for missing productId in BatchItem");
    }

    @Test
    @DisplayName("BatchItem should fail validation when quantity is null")
    void testBatchItemInvalidMissingQuantity() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item = new StoreInventoryBatchAddRequest.BatchItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(null);
        items.add(item);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Quantity is required")),
                "Should have error for missing quantity in BatchItem");
    }

    @Test
    @DisplayName("Global notes can be null (optional)")
    void testGlobalNotesOptional() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        request.setGlobalNotes(null);
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item = new StoreInventoryBatchAddRequest.BatchItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(100);
        items.add(item);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "Global notes should be optional");
    }

    @Test
    @DisplayName("BatchItem notes can be null (optional)")
    void testBatchItemNotesOptional() {
        // Arrange
        StoreInventoryBatchAddRequest request = new StoreInventoryBatchAddRequest();
        request.setStoreId(UUID.randomUUID());
        
        List<StoreInventoryBatchAddRequest.BatchItem> items = new ArrayList<>();
        StoreInventoryBatchAddRequest.BatchItem item = new StoreInventoryBatchAddRequest.BatchItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(100);
        item.setNotes(null);
        items.add(item);
        
        request.setItems(items);
        
        // Act
        Set<ConstraintViolation<StoreInventoryBatchAddRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty(), "Item notes should be optional");
    }
}
