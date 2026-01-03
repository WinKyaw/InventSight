package com.pos.inventsight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVService validation of bulk create items
 * Testing the new validation that requires 4 fields: name, category, unitType, defaultprice
 */
class CSVServiceBulkCreateValidationTest {
    
    private CSVService csvService;
    
    @BeforeEach
    void setUp() {
        csvService = new CSVService();
    }
    
    @Test
    void testValidateItem_AllRequiredFieldsPresent_Valid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertTrue(result);
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testValidateItem_MissingName_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        // name missing
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Name is required"));
    }
    
    @Test
    void testValidateItem_EmptyName_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Name is required"));
    }
    
    @Test
    void testValidateItem_MissingCategory_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        // category missing
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Category is required"));
    }
    
    @Test
    void testValidateItem_EmptyCategory_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Category is required"));
    }
    
    @Test
    void testValidateItem_MissingUnitType_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        // unitType missing
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Unit type is required"));
    }
    
    @Test
    void testValidateItem_MissingDefaultPrice_Invalid() {
        // Given - this is now mandatory
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        // defaultprice missing
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Default price is required"));
    }
    
    @Test
    void testValidateItem_EmptyDefaultPrice_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Default price is required"));
    }
    
    @Test
    void testValidateItem_InvalidPriceFormat_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "not-a-number");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid price format"));
    }
    
    @Test
    void testValidateItem_PriceZero_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "0");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Price must be greater than zero"));
    }
    
    @Test
    void testValidateItem_PriceNegative_Invalid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "-5.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Price must be greater than zero"));
    }
    
    @Test
    void testValidateItem_ValidPositivePrice_Valid() {
        // Given
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertTrue(result);
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testValidateItem_CaseInsensitive_Valid() {
        // Given - mixed case keys
        Map<String, String> item = new HashMap<>();
        item.put("Name", "Apples");
        item.put("CATEGORY", "food");
        item.put("unittype", "lb");  // lowercase
        item.put("DefaultPrice", "2.99");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertTrue(result);
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testValidateItem_MultipleErrors_AllReported() {
        // Given - missing multiple fields
        Map<String, String> item = new HashMap<>();
        // name missing
        // category missing
        item.put("unitType", "lb");
        // defaultprice missing
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertFalse(result);
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Name is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Category is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Default price is required")));
    }
    
    @Test
    void testValidateItem_WithOptionalDescription_Valid() {
        // Given - with optional description field
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        item.put("description", "Fresh red apples");
        
        List<String> errors = new ArrayList<>();
        
        // When
        boolean result = csvService.validateItem(item, errors);
        
        // Then
        assertTrue(result);
        assertTrue(errors.isEmpty());
    }
}
