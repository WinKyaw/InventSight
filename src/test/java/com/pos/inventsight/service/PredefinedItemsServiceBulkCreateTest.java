package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.util.SkuGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PredefinedItemsService bulk create functionality
 * Testing the new validation that requires 4 fields: name, category, unitType, defaultprice
 */
@ExtendWith(MockitoExtension.class)
class PredefinedItemsServiceBulkCreateTest {
    
    @Mock
    private PredefinedItemRepository predefinedItemRepository;
    
    @Mock
    private PredefinedItemStoreRepository predefinedItemStoreRepository;
    
    @Mock
    private PredefinedItemWarehouseRepository predefinedItemWarehouseRepository;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private SupplyManagementService supplyManagementService;
    
    @Mock
    private CSVService csvService;
    
    @Mock
    private SkuGenerator skuGenerator;
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private PredefinedItemsService predefinedItemsService;
    
    private Company company;
    private User user;
    
    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
    }
    
    @Test
    void testBulkCreate_WithAllRequiredFields_Success() {
        // Given - correct format with all 4 required fields
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item1 = new HashMap<>();
        item1.put("name", "Apples");
        item1.put("category", "food");
        item1.put("unitType", "lb");
        item1.put("defaultprice", "2.99");
        itemsData.add(item1);
        
        Map<String, String> item2 = new HashMap<>();
        item2.put("name", "Orange");
        item2.put("category", "food");
        item2.put("unitType", "lb");
        item2.put("defaultprice", "1.99");
        itemsData.add(item2);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(any(), any(), any()))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn("SKU-12345", "SKU-67890");
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(2, result.get("total"));
        assertEquals(2, result.get("successful"));
        assertEquals(0, result.get("failed"));
        assertTrue(((List<?>) result.get("errors")).isEmpty());
        assertEquals(2, ((List<?>) result.get("createdItems")).size());
        
        verify(predefinedItemRepository, times(2)).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_WithSkuField_SkuIsIgnored() {
        // Given - frontend sends 'sku' but it should be ignored
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("sku", "2.99");  // ❌ Wrong field - should be ignored
        item.put("defaultprice", "3.99");  // ✅ Correct field
        itemsData.add(item);
        
        String generatedSku = "AUTO-SKU-123";
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(any(), any(), any()))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem savedItem = invocation.getArgument(0);
                savedItem.setId(UUID.randomUUID());
                return savedItem;
            });
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("successful"));
        assertEquals(0, result.get("failed"));
        
        // Verify SKU was auto-generated (not using "2.99" from sku field)
        verify(skuGenerator, times(1)).generateUniqueSku(any());
    }
    
    @Test
    void testBulkCreate_MissingName_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        // name is missing
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'name' is required"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_MissingCategory_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        // category is missing
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'category' is required"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_MissingUnitType_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        // unitType is missing
        item.put("defaultprice", "2.99");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'unitType' is required"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_MissingDefaultPrice_Fails() {
        // Given - missing defaultprice field
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        // defaultprice is missing
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'defaultprice' is required"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_InvalidPriceFormat_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "invalid");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid price format"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_PriceZero_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "0");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Price must be greater than zero"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_PriceNegative_Fails() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "-1.99");
        itemsData.add(item);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Price must be greater than zero"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_DuplicateItem_Skipped() {
        // Given
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("name", "Apples");
        item.put("category", "food");
        item.put("unitType", "lb");
        item.put("defaultprice", "2.99");
        itemsData.add(item);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, "Apples", "lb"))
            .thenReturn(true);
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Duplicate item"));
        
        verify(predefinedItemRepository, never()).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_MixedValidAndInvalid_ProcessesCorrectly() {
        // Given - mix of valid and invalid items
        List<Map<String, String>> itemsData = new ArrayList<>();
        
        // Valid item 1
        Map<String, String> validItem1 = new HashMap<>();
        validItem1.put("name", "Apples");
        validItem1.put("category", "food");
        validItem1.put("unitType", "lb");
        validItem1.put("defaultprice", "2.99");
        itemsData.add(validItem1);
        
        // Invalid item - missing price
        Map<String, String> invalidItem = new HashMap<>();
        invalidItem.put("name", "Oranges");
        invalidItem.put("category", "food");
        invalidItem.put("unitType", "lb");
        itemsData.add(invalidItem);
        
        // Valid item 2
        Map<String, String> validItem2 = new HashMap<>();
        validItem2.put("name", "Bananas");
        validItem2.put("category", "food");
        validItem2.put("unitType", "lb");
        validItem2.put("defaultprice", "1.50");
        itemsData.add(validItem2);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(any(), any(), any()))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn("SKU-1", "SKU-2");
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(3, result.get("total"));
        assertEquals(2, result.get("successful"));
        assertEquals(1, result.get("failed"));
        
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Row 2"));
        assertTrue(errors.get(0).contains("'defaultprice' is required"));
        
        verify(predefinedItemRepository, times(2)).save(any(PredefinedItem.class));
    }
    
    @Test
    void testBulkCreate_CaseInsensitiveKeys_Success() {
        // Given - mixed case keys
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("Name", "Apples");  // Capital N
        item.put("CATEGORY", "food");  // All caps
        item.put("unittype", "lb");  // lowercase
        item.put("DefaultPrice", "2.99");  // Mixed case
        itemsData.add(item);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(any(), any(), any()))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn("SKU-12345");
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem savedItem = invocation.getArgument(0);
                savedItem.setId(UUID.randomUUID());
                return savedItem;
            });
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, null, null
        );
        
        // Then
        assertEquals(1, result.get("successful"));
        assertEquals(0, result.get("failed"));
        
        verify(predefinedItemRepository, times(1)).save(any(PredefinedItem.class));
    }
}
