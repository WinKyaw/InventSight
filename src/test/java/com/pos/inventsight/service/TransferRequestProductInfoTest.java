package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test to verify that Product information is populated in TransferRequest
 */
public class TransferRequestProductInfoTest {
    
    @Mock
    private TransferRequestRepository transferRequestRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private TransferLocationRepository transferLocationRepository;
    
    @InjectMocks
    private TransferRequestService transferRequestService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    public void testCreateEnhancedTransferRequest_PopulatesProductInfo() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID fromLocationId = UUID.randomUUID();
        UUID toLocationId = UUID.randomUUID();
        
        // Create mock Product
        Product product = new Product();
        product.setId(productId);
        product.setName("Soda");
        product.setSku("DRINK-001");
        
        // Create mock Company
        Company company = new Company();
        company.setId(UUID.randomUUID());
        
        // Create mock User
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        
        // Create mock Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setId(fromLocationId);
        
        // Create mock Store
        Store store = new Store();
        store.setId(toLocationId);
        
        // Create TransferRequest
        TransferRequest request = new TransferRequest();
        request.setProductId(productId);
        request.setFromLocationType("WAREHOUSE");
        request.setFromLocationId(fromLocationId);
        request.setToLocationType("STORE");
        request.setToLocationId(toLocationId);
        request.setRequestedQuantity(100);
        
        // Mock repository behaviors
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(fromLocationId)).thenReturn(Optional.of(warehouse));
        when(storeRepository.findById(toLocationId)).thenReturn(Optional.of(store));
        
        // Mock TransferLocationRepository
        TransferLocation fromTransferLocation = new TransferLocation(warehouse);
        TransferLocation toTransferLocation = new TransferLocation(store);
        when(transferLocationRepository.findByWarehouseId(fromLocationId)).thenReturn(Optional.empty());
        when(transferLocationRepository.findByStoreId(toLocationId)).thenReturn(Optional.empty());
        when(transferLocationRepository.save(any(TransferLocation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        TransferRequest result = transferRequestService.createEnhancedTransferRequest(request, company, user);
        
        // Assert
        assertNotNull(result);
        assertEquals("Soda", result.getProductName(), "Product name should be populated");
        assertEquals("DRINK-001", result.getProductSku(), "Product SKU should be populated");
        assertEquals("Soda", result.getItemName(), "Item name should be populated");
        assertEquals("DRINK-001", result.getItemSku(), "Item SKU should be populated");
        assertEquals(productId, result.getProductId(), "Product ID should be set");
        assertEquals(100, result.getRequestedQuantity(), "Requested quantity should be set");
        
        // Verify product was fetched
        verify(productRepository, times(1)).findById(productId);
    }
    
    @Test
    public void testCreateEnhancedTransferRequest_ThrowsExceptionForInvalidProduct() {
        // Arrange
        UUID invalidProductId = UUID.randomUUID();
        UUID fromLocationId = UUID.randomUUID();
        UUID toLocationId = UUID.randomUUID();
        
        Company company = new Company();
        company.setId(UUID.randomUUID());
        
        User user = new User();
        user.setId(UUID.randomUUID());
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(fromLocationId);
        
        Store store = new Store();
        store.setId(toLocationId);
        
        TransferRequest request = new TransferRequest();
        request.setProductId(invalidProductId);
        request.setFromLocationType("WAREHOUSE");
        request.setFromLocationId(fromLocationId);
        request.setToLocationType("STORE");
        request.setToLocationId(toLocationId);
        request.setRequestedQuantity(50);
        
        // Mock repository behaviors
        when(productRepository.findById(invalidProductId)).thenReturn(Optional.empty());
        when(warehouseRepository.findById(fromLocationId)).thenReturn(Optional.of(warehouse));
        when(storeRepository.findById(toLocationId)).thenReturn(Optional.of(store));
        
        // Mock TransferLocationRepository for validation
        when(transferLocationRepository.findByWarehouseId(fromLocationId)).thenReturn(Optional.empty());
        when(transferLocationRepository.findByStoreId(toLocationId)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> transferRequestService.createEnhancedTransferRequest(request, company, user),
            "Should throw ResourceNotFoundException for invalid product ID"
        );
        
        assertTrue(exception.getMessage().contains("Product not found with ID"));
        
        // Verify product repository was called
        verify(productRepository, times(1)).findById(invalidProductId);
        // Verify save was NOT called
        verify(transferRequestRepository, never()).save(any(TransferRequest.class));
    }
    
    @Test
    public void testCreateTransferRequest_PopulatesProductInfo() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        
        // Create mock Product
        Product product = new Product();
        product.setId(productId);
        product.setName("Laptop");
        product.setSku("TECH-001");
        
        // Create mock Company
        Company company = new Company();
        company.setId(UUID.randomUUID());
        
        // Create mock User
        User user = new User();
        user.setId(UUID.randomUUID());
        
        // Create mock Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        
        // Create mock Store
        Store store = new Store();
        store.setId(storeId);
        
        // Create TransferRequest
        TransferRequest request = new TransferRequest();
        request.setProductId(productId);
        request.setRequestedQuantity(10);
        
        // Mock repository behaviors
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        TransferRequest result = transferRequestService.createTransferRequest(request, company, warehouse, store, user);
        
        // Assert
        assertNotNull(result);
        assertEquals("Laptop", result.getProductName(), "Product name should be populated");
        assertEquals("TECH-001", result.getProductSku(), "Product SKU should be populated");
        assertEquals("Laptop", result.getItemName(), "Item name should be populated");
        assertEquals("TECH-001", result.getItemSku(), "Item SKU should be populated");
        
        // Verify product was fetched
        verify(productRepository, times(1)).findById(productId);
    }
}
