package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
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
 * Unit tests for PredefinedItemsService with store/warehouse association functionality
 */
@ExtendWith(MockitoExtension.class)
class PredefinedItemsServiceAssociationTest {
    
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
    private Store store1;
    private Store store2;
    private Warehouse warehouse1;
    private Warehouse warehouse2;
    
    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        
        store1 = new Store();
        store1.setId(UUID.randomUUID());
        store1.setStoreName("Store 1");
        store1.setCompany(company);
        
        store2 = new Store();
        store2.setId(UUID.randomUUID());
        store2.setStoreName("Store 2");
        store2.setCompany(company);
        
        warehouse1 = new Warehouse();
        warehouse1.setId(UUID.randomUUID());
        warehouse1.setName("Warehouse 1");
        warehouse1.setCompany(company);
        
        warehouse2 = new Warehouse();
        warehouse2.setId(UUID.randomUUID());
        warehouse2.setName("Warehouse 2");
        warehouse2.setCompany(company);
    }
    
    @Test
    void testCreateItem_WithoutAssociations_Success() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PredefinedItem result = predefinedItemsService.createItem(
            name, null, "Category", unitType, "Description", 
            new BigDecimal("10.00"), company, user, null, null
        );
        
        // Then
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(generatedSku, result.getSku());
        
        verify(predefinedItemRepository).save(any(PredefinedItem.class));
        verify(predefinedItemStoreRepository, never()).save(any(PredefinedItemStore.class));
        verify(predefinedItemWarehouseRepository, never()).save(any(PredefinedItemWarehouse.class));
    }
    
    @Test
    void testCreateItem_WithStoresOnly_Success() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        List<UUID> storeIds = Arrays.asList(store1.getId(), store2.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(storeRepository.findByIdWithCompany(store2.getId())).thenReturn(Optional.of(store2));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PredefinedItem result = predefinedItemsService.createItem(
            name, null, "Category", unitType, "Description", 
            new BigDecimal("10.00"), company, user, storeIds, null
        );
        
        // Then
        assertNotNull(result);
        verify(predefinedItemStoreRepository, times(1)).deleteByPredefinedItem(any());
        verify(predefinedItemStoreRepository, times(2)).save(any(PredefinedItemStore.class));
        verify(predefinedItemWarehouseRepository, never()).save(any(PredefinedItemWarehouse.class));
    }
    
    @Test
    void testCreateItem_WithWarehousesOnly_Success() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        List<UUID> warehouseIds = Arrays.asList(warehouse1.getId(), warehouse2.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(warehouseRepository.findByIdWithCompany(warehouse2.getId())).thenReturn(Optional.of(warehouse2));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PredefinedItem result = predefinedItemsService.createItem(
            name, null, "Category", unitType, "Description", 
            new BigDecimal("10.00"), company, user, null, warehouseIds
        );
        
        // Then
        assertNotNull(result);
        verify(predefinedItemWarehouseRepository, times(1)).deleteByPredefinedItem(any());
        verify(predefinedItemWarehouseRepository, times(2)).save(any(PredefinedItemWarehouse.class));
        verify(predefinedItemStoreRepository, never()).save(any(PredefinedItemStore.class));
    }
    
    @Test
    void testCreateItem_WithBothStoresAndWarehouses_Success() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        List<UUID> storeIds = Arrays.asList(store1.getId());
        List<UUID> warehouseIds = Arrays.asList(warehouse1.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any())).thenReturn(Optional.empty());
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PredefinedItem result = predefinedItemsService.createItem(
            name, null, "Category", unitType, "Description", 
            new BigDecimal("10.00"), company, user, storeIds, warehouseIds
        );
        
        // Then
        assertNotNull(result);
        verify(predefinedItemStoreRepository, times(1)).deleteByPredefinedItem(any());
        verify(predefinedItemStoreRepository, times(1)).save(any(PredefinedItemStore.class));
        verify(predefinedItemWarehouseRepository, times(1)).deleteByPredefinedItem(any());
        verify(predefinedItemWarehouseRepository, times(1)).save(any(PredefinedItemWarehouse.class));
    }
    
    @Test
    void testCreateItem_WithInvalidStoreId_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        UUID invalidStoreId = UUID.randomUUID();
        List<UUID> storeIds = Arrays.asList(invalidStoreId);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(invalidStoreId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, storeIds, null
            );
        });
    }
    
    @Test
    void testCreateItem_WithInvalidWarehouseId_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        UUID invalidWarehouseId = UUID.randomUUID();
        List<UUID> warehouseIds = Arrays.asList(invalidWarehouseId);
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(warehouseRepository.findByIdWithCompany(invalidWarehouseId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, null, warehouseIds
            );
        });
    }
    
    @Test
    void testCreateItem_WithStoreDifferentCompany_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        
        Company differentCompany = new Company();
        differentCompany.setId(UUID.randomUUID());
        differentCompany.setName("Different Company");
        
        Store differentStore = new Store();
        differentStore.setId(UUID.randomUUID());
        differentStore.setStoreName("Different Store");
        differentStore.setCompany(differentCompany);
        
        List<UUID> storeIds = Arrays.asList(differentStore.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(differentStore.getId())).thenReturn(Optional.of(differentStore));
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, storeIds, null
            );
        });
    }
    
    @Test
    void testCreateItem_WithWarehouseDifferentCompany_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        
        Company differentCompany = new Company();
        differentCompany.setId(UUID.randomUUID());
        differentCompany.setName("Different Company");
        
        Warehouse differentWarehouse = new Warehouse();
        differentWarehouse.setId(UUID.randomUUID());
        differentWarehouse.setName("Different Warehouse");
        differentWarehouse.setCompany(differentCompany);
        
        List<UUID> warehouseIds = Arrays.asList(differentWarehouse.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(warehouseRepository.findByIdWithCompany(differentWarehouse.getId()))
            .thenReturn(Optional.of(differentWarehouse));
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, null, warehouseIds
            );
        });
    }
    
    @Test
    void testCreateItem_WithEmptyStoreList_NoAssociations() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        List<UUID> emptyStoreIds = new ArrayList<>();
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PredefinedItem result = predefinedItemsService.createItem(
            name, null, "Category", unitType, "Description", 
            new BigDecimal("10.00"), company, user, emptyStoreIds, null
        );
        
        // Then
        assertNotNull(result);
        verify(predefinedItemStoreRepository, never()).deleteByPredefinedItem(any());
        verify(predefinedItemStoreRepository, never()).save(any(PredefinedItemStore.class));
    }
    
    @Test
    void testCreateItem_WithStoreHavingNullCompany_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        
        Store storeWithNoCompany = new Store();
        storeWithNoCompany.setId(UUID.randomUUID());
        storeWithNoCompany.setStoreName("Store Without Company");
        storeWithNoCompany.setCompany(null); // Null company
        
        List<UUID> storeIds = Arrays.asList(storeWithNoCompany.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(storeWithNoCompany.getId()))
            .thenReturn(Optional.of(storeWithNoCompany));
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, storeIds, null
            );
        });
    }
    
    @Test
    void testCreateItem_WithWarehouseHavingNullCompany_ThrowsException() {
        // Given
        String name = "Test Item";
        String unitType = "PCS";
        String generatedSku = "SKU-12345";
        
        Warehouse warehouseWithNoCompany = new Warehouse();
        warehouseWithNoCompany.setId(UUID.randomUUID());
        warehouseWithNoCompany.setName("Warehouse Without Company");
        warehouseWithNoCompany.setCompany(null); // Null company
        
        List<UUID> warehouseIds = Arrays.asList(warehouseWithNoCompany.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(company, name, unitType))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn(generatedSku);
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(warehouseRepository.findByIdWithCompany(warehouseWithNoCompany.getId()))
            .thenReturn(Optional.of(warehouseWithNoCompany));
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            predefinedItemsService.createItem(
                name, null, "Category", unitType, "Description", 
                new BigDecimal("10.00"), company, user, null, warehouseIds
            );
        });
    }
    
    @Test
    void testBulkCreateItems_WithAssociations_Success() {
        // Given - now requires all 4 fields: name, category, unitType, defaultprice
        List<Map<String, String>> itemsData = new ArrayList<>();
        Map<String, String> item1 = new HashMap<>();
        item1.put("name", "Item 1");
        item1.put("category", "Category 1");
        item1.put("unitType", "PCS");
        item1.put("defaultprice", "10.00");
        itemsData.add(item1);
        
        Map<String, String> item2 = new HashMap<>();
        item2.put("name", "Item 2");
        item2.put("category", "Category 2");
        item2.put("unitType", "KG");
        item2.put("defaultprice", "20.00");
        itemsData.add(item2);
        
        List<UUID> storeIds = Arrays.asList(store1.getId());
        List<UUID> warehouseIds = Arrays.asList(warehouse1.getId());
        
        when(predefinedItemRepository.existsByCompanyAndNameAndUnitType(any(), any(), any()))
            .thenReturn(false);
        when(skuGenerator.generateUniqueSku(any())).thenReturn("SKU-12345", "SKU-67890");
        when(predefinedItemRepository.save(any(PredefinedItem.class)))
            .thenAnswer(invocation -> {
                PredefinedItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any())).thenReturn(Optional.empty());
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Map<String, Object> result = predefinedItemsService.bulkCreateItems(
            itemsData, company, user, storeIds, warehouseIds
        );
        
        // Then
        assertEquals(2, result.get("total"));
        assertEquals(2, result.get("successful"));
        assertEquals(0, result.get("failed"));
        
        // Each item should have associations created
        verify(predefinedItemStoreRepository, times(2)).deleteByPredefinedItem(any());
        verify(predefinedItemStoreRepository, times(2)).save(any(PredefinedItemStore.class));
        verify(predefinedItemWarehouseRepository, times(2)).deleteByPredefinedItem(any());
        verify(predefinedItemWarehouseRepository, times(2)).save(any(PredefinedItemWarehouse.class));
    }
}
