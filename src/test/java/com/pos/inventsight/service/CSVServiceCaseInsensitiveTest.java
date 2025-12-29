package com.pos.inventsight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CSV Service Case-Insensitive Field Validation Tests")
class CSVServiceCaseInsensitiveTest {

    private CSVService csvService;

    @BeforeEach
    void setUp() {
        csvService = new CSVService();
    }

    @Test
    @DisplayName("Should accept camelCase unitType field from frontend")
    void testValidateItemWithCamelCaseUnitType() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "apple");
        item.put("category", "food");
        item.put("unitType", "lb");  // camelCase from frontend

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with camelCase unitType");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    @DisplayName("Should accept lowercase unittype field from CSV")
    void testValidateItemWithLowercaseUnittype() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "banana");
        item.put("category", "food");
        item.put("unittype", "kg");  // lowercase from CSV

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with lowercase unittype");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    @DisplayName("Should accept mixed case field names")
    void testValidateItemWithMixedCase() {
        Map<String, String> item = new HashMap<>();
        item.put("Name", "Orange");       // Title case
        item.put("CATEGORY", "Fruit");    // Upper case
        item.put("UnitType", "box");      // Mixed case

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with mixed case field names");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    @DisplayName("Should validate price with case-insensitive field name")
    void testValidateItemWithCamelCaseDefaultPrice() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "grape");
        item.put("unitType", "lb");
        item.put("defaultPrice", "5.99");  // camelCase

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with camelCase defaultPrice");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    @DisplayName("Should validate price with lowercase field name")
    void testValidateItemWithLowercaseDefaultPrice() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "watermelon");
        item.put("unittype", "ea");
        item.put("defaultprice", "3.50");  // lowercase

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with lowercase defaultprice");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    @DisplayName("Should reject invalid price format regardless of case")
    void testValidateItemWithInvalidPrice() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "strawberry");
        item.put("unitType", "lb");
        item.put("defaultPrice", "invalid");  // Invalid price format

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertFalse(result, "Validation should fail with invalid price format");
        assertFalse(errors.isEmpty(), "Should have validation errors");
        assertTrue(errors.get(0).contains("Invalid price format"), 
                  "Error message should mention invalid price format");
    }

    @Test
    @DisplayName("Should require name field regardless of case")
    void testValidateItemMissingName() {
        Map<String, String> item = new HashMap<>();
        item.put("unitType", "lb");
        // Missing name field

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertFalse(result, "Validation should fail without name");
        assertFalse(errors.isEmpty(), "Should have validation errors");
        assertTrue(errors.get(0).contains("Name is required"), 
                  "Error message should mention name is required");
    }

    @Test
    @DisplayName("Should require unitType field regardless of case")
    void testValidateItemMissingUnitType() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "pineapple");
        // Missing unitType field

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertFalse(result, "Validation should fail without unitType");
        assertFalse(errors.isEmpty(), "Should have validation errors");
        assertTrue(errors.get(0).contains("Unit type is required"), 
                  "Error message should mention unit type is required");
    }

    @Test
    @DisplayName("Should accept empty optional fields with case-insensitive names")
    void testValidateItemWithEmptyOptionalFields() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "mango");
        item.put("unitType", "ea");
        item.put("category", "");       // Empty optional field
        item.put("description", "");    // Empty optional field
        item.put("defaultPrice", "");   // Empty optional field (should not validate)

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with empty optional fields");
        assertTrue(errors.isEmpty(), "Should have no validation errors for empty optional fields");
    }

    @Test
    @DisplayName("Should handle all field variations in a single item")
    void testValidateItemWithAllFieldVariations() {
        Map<String, String> item = new HashMap<>();
        item.put("Name", "Kiwi");           // Title case
        item.put("SKU", "KW-001");          // Upper case
        item.put("Category", "Fruit");      // Title case
        item.put("unitType", "box");        // camelCase
        item.put("Description", "Fresh kiwis"); // Title case
        item.put("defaultPrice", "4.99");   // camelCase

        List<String> errors = new ArrayList<>();
        boolean result = csvService.validateItem(item, errors);

        assertTrue(result, "Validation should succeed with various field name cases");
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }
}
