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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for per-location SKU uniqueness
 * Verifies that the same SKU can exist in different stores/warehouses
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Per-Location SKU Uniqueness Tests")
class PerLocationSkuUniquenessTest {
    
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
    private PredefinedItem predefinedItem;
    
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
        
        predefinedItem = new PredefinedItem("Apple", "PCS", company, user);
        predefinedItem.setId(UUID.randomUUID());
        predefinedItem.setSku("69368646550"); // Same SKU from the problem statement
        predefinedItem.setCategory("Fruits");
        predefinedItem.setDescription("Fresh Apple");
        predefinedItem.setDefaultPrice(new BigDecimal("10.00"));
    }
    
    @Test
    @DisplayName("Same SKU should be allowed in different stores")
    void testSameSkuInDifferentStores() {
        // Given
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(storeRepository.findByIdWithCompany(store2.getId())).thenReturn(Optional.of(store2));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any()))
            .thenReturn(Optional.empty());
        
        // Capture all products being saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture()))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When - Associate with both stores
        predefinedItemsService.associateStores(
            predefinedItem, 
            Arrays.asList(store1.getId()), 
            user
        );
        
        predefinedItemsService.associateStores(
            predefinedItem, 
            Arrays.asList(store2.getId()), 
            user
        );
        
        // Then - Both products should be created with same SKU
        List<Product> capturedProducts = productCaptor.getAllValues();
        assertEquals(2, capturedProducts.size());
        
        Product product1 = capturedProducts.get(0);
        Product product2 = capturedProducts.get(1);
        
        // Verify both have same SKU
        assertEquals("69368646550", product1.getSku());
        assertEquals("69368646550", product2.getSku());
        
        // But different stores
        assertEquals(store1.getId(), product1.getStore().getId());
        assertEquals(store2.getId(), product2.getStore().getId());
        
        // And both have null warehouse
        assertNull(product1.getWarehouse());
        assertNull(product2.getWarehouse());
    }
    
    @Test
    @DisplayName("Same SKU should be allowed in different warehouses")
    void testSameSkuInDifferentWarehouses() {
        // Given
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(warehouseRepository.findByIdWithCompany(warehouse2.getId())).thenReturn(Optional.of(warehouse2));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any()))
            .thenReturn(Optional.empty());
        
        // Capture all products being saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture()))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When - Associate with both warehouses
        predefinedItemsService.associateWarehouses(
            predefinedItem, 
            Arrays.asList(warehouse1.getId()), 
            user
        );
        
        predefinedItemsService.associateWarehouses(
            predefinedItem, 
            Arrays.asList(warehouse2.getId()), 
            user
        );
        
        // Then - Both products should be created with same SKU
        List<Product> capturedProducts = productCaptor.getAllValues();
        assertEquals(2, capturedProducts.size());
        
        Product product1 = capturedProducts.get(0);
        Product product2 = capturedProducts.get(1);
        
        // Verify both have same SKU
        assertEquals("69368646550", product1.getSku());
        assertEquals("69368646550", product2.getSku());
        
        // But different warehouses
        assertEquals(warehouse1.getId(), product1.getWarehouse().getId());
        assertEquals(warehouse2.getId(), product2.getWarehouse().getId());
        
        // And both have null store
        assertNull(product1.getStore());
        assertNull(product2.getStore());
    }
    
    @Test
    @DisplayName("Same SKU should be allowed in both store and warehouse")
    void testSameSkuInStoreAndWarehouse() {
        // Given
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(any(), any()))
            .thenReturn(Optional.empty());
        when(productRepository.findByPredefinedItemAndWarehouse(any(), any()))
            .thenReturn(Optional.empty());
        
        // Capture all products being saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture()))
            .thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
        
        // When - Associate with both store and warehouse
        predefinedItemsService.associateStores(
            predefinedItem, 
            Arrays.asList(store1.getId()), 
            user
        );
        
        predefinedItemsService.associateWarehouses(
            predefinedItem, 
            Arrays.asList(warehouse1.getId()), 
            user
        );
        
        // Then - Both products should be created with same SKU
        List<Product> capturedProducts = productCaptor.getAllValues();
        assertEquals(2, capturedProducts.size());
        
        Product storeProduct = capturedProducts.get(0);
        Product warehouseProduct = capturedProducts.get(1);
        
        // Verify both have same SKU
        assertEquals("69368646550", storeProduct.getSku());
        assertEquals("69368646550", warehouseProduct.getSku());
        
        // But different location types
        assertNotNull(storeProduct.getStore());
        assertEquals(store1.getId(), storeProduct.getStore().getId());
        assertNull(storeProduct.getWarehouse());
        
        assertNotNull(warehouseProduct.getWarehouse());
        assertEquals(warehouse1.getId(), warehouseProduct.getWarehouse().getId());
        assertNull(warehouseProduct.getStore());
    }
    
    @Test
    @DisplayName("Duplicate assignment to same store should return existing product")
    void testDuplicateAssignmentToSameStore() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(UUID.randomUUID());
        existingProduct.setSku("69368646550");
        existingProduct.setStore(store1);
        existingProduct.setPredefinedItem(predefinedItem);
        
        when(storeRepository.findByIdWithCompany(store1.getId())).thenReturn(Optional.of(store1));
        when(predefinedItemStoreRepository.save(any(PredefinedItemStore.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndStore(predefinedItem, store1))
            .thenReturn(Optional.of(existingProduct));
        
        // When - Try to associate with Store 1 again
        predefinedItemsService.associateStores(
            predefinedItem, 
            Arrays.asList(store1.getId()), 
            user
        );
        
        // Then - Should not create a new product
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, times(1)).findByPredefinedItemAndStore(predefinedItem, store1);
    }
    
    @Test
    @DisplayName("Duplicate assignment to same warehouse should return existing product")
    void testDuplicateAssignmentToSameWarehouse() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(UUID.randomUUID());
        existingProduct.setSku("69368646550");
        existingProduct.setWarehouse(warehouse1);
        existingProduct.setPredefinedItem(predefinedItem);
        
        when(warehouseRepository.findByIdWithCompany(warehouse1.getId())).thenReturn(Optional.of(warehouse1));
        when(predefinedItemWarehouseRepository.save(any(PredefinedItemWarehouse.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByPredefinedItemAndWarehouse(predefinedItem, warehouse1))
            .thenReturn(Optional.of(existingProduct));
        
        // When - Try to associate with Warehouse 1 again
        predefinedItemsService.associateWarehouses(
            predefinedItem, 
            Arrays.asList(warehouse1.getId()), 
            user
        );
        
        // Then - Should not create a new product
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, times(1)).findByPredefinedItemAndWarehouse(predefinedItem, warehouse1);
    }
}
