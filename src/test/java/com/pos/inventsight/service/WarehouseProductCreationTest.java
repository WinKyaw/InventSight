package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.util.SkuGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for warehouse-only product creation
 * Verifies that products can be created with warehouse_id set and store_id NULL
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Warehouse Product Creation Tests")
class WarehouseProductCreationTest {
    
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
    private Warehouse warehouse;
    private PredefinedItem predefinedItem;
    
    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        
        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Test Warehouse");
        warehouse.setCompany(company);
        
        predefinedItem = new PredefinedItem("Test Item", "PCS", company, user);
        predefinedItem.setId(UUID.randomUUID());
        predefinedItem.setSku("SKU-12345");
        predefinedItem.setCategory("Category");
        predefinedItem.setDescription("Description");
        predefinedItem.setDefaultPrice(new BigDecimal("10.00"));
    }
    
    @Test
    @DisplayName("Warehouse-only product should have store_id = NULL and warehouse_id set")
    void testWarehouseOnlyProductCreation() {
        // Given
        when(warehouseRepository.findByIdWithCompany(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any()))
            .thenReturn(Optional.empty());
        
        // Capture the product being saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture()))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When
        predefinedItemsService.associateWarehouses(
            predefinedItem, 
            Arrays.asList(warehouse.getId()), 
            user
        );
        
        // Then
        Product savedProduct = productCaptor.getValue();
        
        // Verify product has warehouse set
        assertNotNull(savedProduct.getWarehouse(), "Warehouse should be set");
        assertEquals(warehouse.getId(), savedProduct.getWarehouse().getId(), 
            "Warehouse ID should match");
        
        // Verify product has store_id = NULL
        assertNull(savedProduct.getStore(), 
            "Store should be NULL for warehouse-only product");
        
        // Verify company is set from warehouse
        assertNotNull(savedProduct.getCompany(), "Company should be set");
        assertEquals(company.getId(), savedProduct.getCompany().getId(), 
            "Company should match warehouse's company");
        
        // Verify product properties are correctly copied
        assertEquals(predefinedItem.getName(), savedProduct.getName());
        assertEquals(predefinedItem.getSku(), savedProduct.getSku());
        assertEquals(predefinedItem.getCategory(), savedProduct.getCategory());
        assertEquals(predefinedItem.getUnitType(), savedProduct.getUnit());
        assertEquals(predefinedItem.getDescription(), savedProduct.getDescription());
        assertEquals(predefinedItem.getDefaultPrice(), savedProduct.getRetailPrice());
        
        // Verify initial stock is 0
        assertEquals(0, savedProduct.getQuantity());
        
        // Verify it's active
        assertTrue(savedProduct.getIsActive());
    }
    
    @Test
    @DisplayName("Store-only product should have warehouse_id = NULL and store_id set")
    void testStoreOnlyProductCreation() {
        // Given
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        store.setCompany(company);
        
        when(storeRepository.findByIdWithCompany(store.getId())).thenReturn(Optional.of(store));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any()))
            .thenReturn(Optional.empty());
        
        // Capture the product being saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture()))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When
        predefinedItemsService.associateStores(
            predefinedItem, 
            Arrays.asList(store.getId()), 
            user
        );
        
        // Then
        Product savedProduct = productCaptor.getValue();
        
        // Verify product has store set
        assertNotNull(savedProduct.getStore(), "Store should be set");
        assertEquals(store.getId(), savedProduct.getStore().getId(), 
            "Store ID should match");
        
        // Verify product has warehouse_id = NULL
        assertNull(savedProduct.getWarehouse(), 
            "Warehouse should be NULL for store-only product");
        
        // Verify company is set
        assertNotNull(savedProduct.getCompany(), "Company should be set");
    }
    
    @Test
    @DisplayName("Product creation should not fail when store_id is NULL")
    void testWarehouseProductDoesNotRequireStore() {
        // Given
        when(warehouseRepository.findByIdWithCompany(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any()))
            .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                // This would trigger @PrePersist validation
                p.validateLocation();
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> {
            predefinedItemsService.associateWarehouses(
                predefinedItem, 
                Arrays.asList(warehouse.getId()), 
                user
            );
        }, "Creating warehouse-only product should not throw exception");
        
        // Verify product was saved
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
