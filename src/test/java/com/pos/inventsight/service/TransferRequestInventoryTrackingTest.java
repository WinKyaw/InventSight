package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Transfer Request Inventory Tracking
 * Tests the new functionality for creating restock history records and logging warehouse shipments
 */
@ExtendWith(MockitoExtension.class)
public class TransferRequestInventoryTrackingTest {

    @Mock
    private TransferRequestRepository transferRequestRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private StoreInventoryAdditionRepository additionRepository;

    @Mock
    private WarehouseInventoryWithdrawalRepository withdrawalRepository;

    @InjectMocks
    private TransferRequestService transferRequestService;

    private TransferRequest testTransfer;
    private Product testProduct;
    private Store testStore;
    private Warehouse testWarehouse;
    private User testUser;
    private Company testCompany;

    @BeforeEach
    public void setUp() {
        // Setup test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");

        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        // Setup test warehouse
        testWarehouse = new Warehouse();
        testWarehouse.setId(UUID.randomUUID());
        testWarehouse.setName("Test Warehouse");

        // Setup test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");

        // Setup test product
        testProduct = new Product();
        testProduct.setId(UUID.randomUUID());
        testProduct.setName("Test Product");
        testProduct.setSku("TEST-SKU-001");
        testProduct.setQuantity(50);
        testProduct.setStore(testStore);

        // Setup test transfer
        testTransfer = new TransferRequest();
        testTransfer.setId(UUID.randomUUID());
        testTransfer.setCompany(testCompany);
        testTransfer.setProductId(testProduct.getId());
        testTransfer.setProductName(testProduct.getName());
        testTransfer.setProductSku(testProduct.getSku());
        testTransfer.setFromWarehouse(testWarehouse);
        testTransfer.setFromLocationType("WAREHOUSE");
        testTransfer.setFromLocationId(testWarehouse.getId());
        testTransfer.setToStore(testStore);
        testTransfer.setToLocationType("STORE");
        testTransfer.setToLocationId(testStore.getId());
        testTransfer.setRequestedQuantity(100);
        testTransfer.setApprovedQuantity(100);
        testTransfer.setReceivedQuantity(100);
        testTransfer.setDamagedQuantity(0);
        testTransfer.setReceivedByUser(testUser);
        testTransfer.setStatus(TransferRequestStatus.COMPLETED);
    }

    @Test
    public void testStoreRestockHistoryRecordCreated() {
        // Given
        WarehouseInventory warehouseInventory = new WarehouseInventory();
        warehouseInventory.setWarehouse(testWarehouse);
        warehouseInventory.setProduct(testProduct);
        warehouseInventory.setCurrentQuantity(200);

        lenient().when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        // Mock findBySkuAndStoreId to return the existing store product
        lenient().when(productRepository.findBySkuAndStoreId(testProduct.getSku(), testStore.getId()))
            .thenReturn(Optional.of(testProduct));
        lenient().when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));
        lenient().when(warehouseRepository.findById(testWarehouse.getId()))
            .thenReturn(Optional.of(testWarehouse));
        lenient().when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));

        // Capture the StoreInventoryAddition that gets saved
        StoreInventoryAddition[] capturedAddition = new StoreInventoryAddition[1];
        when(additionRepository.save(any(StoreInventoryAddition.class)))
            .thenAnswer(invocation -> {
                capturedAddition[0] = invocation.getArgument(0);
                return capturedAddition[0];
            });

        // When - simulate receiving transfer at store (good quantity = received - damaged)
        // This would normally be called by receiveTransfer() method
        int goodQuantity = testTransfer.getReceivedQuantity() - 
                          (testTransfer.getDamagedQuantity() != null ? testTransfer.getDamagedQuantity() : 0);
        
        // Simulate the inventory update process
        try {
            // Use reflection to access private method for testing
            java.lang.reflect.Method method = TransferRequestService.class.getDeclaredMethod(
                "addToStoreInventory", UUID.class, Integer.class, TransferRequest.class);
            method.setAccessible(true);
            method.invoke(transferRequestService, testProduct.getId(), goodQuantity, testTransfer);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        // Then - verify restock history record was created
        assertNotNull(capturedAddition[0], "Restock history record should be created");
        assertEquals(testStore, capturedAddition[0].getStore(), 
            "Restock record should be for the correct store");
        assertEquals(testProduct, capturedAddition[0].getProduct(), 
            "Restock record should be for the correct product");
        assertEquals(goodQuantity, capturedAddition[0].getQuantity(), 
            "Restock record should have the correct quantity");
        assertEquals(StoreInventoryAddition.TransactionType.TRANSFER_IN, 
            capturedAddition[0].getTransactionType(), 
            "Restock record should be marked as TRANSFER_IN");
        assertTrue(capturedAddition[0].getReferenceNumber().startsWith("TRANSFER-"), 
            "Reference number should start with TRANSFER-");
        assertTrue(capturedAddition[0].getNotes().contains("transfer request"), 
            "Notes should mention transfer request");
        assertTrue(capturedAddition[0].getNotes().contains(testWarehouse.getName()), 
            "Notes should include source warehouse name");
        assertEquals(testUser.getUsername(), capturedAddition[0].getCreatedBy(), 
            "Created by should be the receiving user");
        assertEquals(StoreInventoryAddition.TransactionStatus.COMPLETED, 
            capturedAddition[0].getStatus(), 
            "Status should be COMPLETED");
        assertEquals(LocalDate.now(), capturedAddition[0].getReceiptDate(), 
            "Receipt date should be today");

        // Verify product quantity was updated
        verify(productRepository, times(1)).save(testProduct);
        assertEquals(150, testProduct.getQuantity(), 
            "Product quantity should be increased by good quantity");
    }

    @Test
    public void testWarehouseShipmentLogged() {
        // Given
        WarehouseInventory warehouseInventory = new WarehouseInventory();
        warehouseInventory.setWarehouse(testWarehouse);
        warehouseInventory.setProduct(testProduct);
        warehouseInventory.setCurrentQuantity(200);

        // Set up approved user for transfer
        User approvedUser = new User();
        approvedUser.setId(UUID.randomUUID());
        approvedUser.setUsername("approver");
        testTransfer.setApprovedBy(approvedUser);

        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));
        when(warehouseRepository.findById(testWarehouse.getId()))
            .thenReturn(Optional.of(testWarehouse));
        when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));

        // Capture the WarehouseInventoryWithdrawal that gets saved
        WarehouseInventoryWithdrawal[] capturedWithdrawal = new WarehouseInventoryWithdrawal[1];
        when(withdrawalRepository.save(any(WarehouseInventoryWithdrawal.class)))
            .thenAnswer(invocation -> {
                capturedWithdrawal[0] = invocation.getArgument(0);
                return capturedWithdrawal[0];
            });

        // When - simulate deducting from warehouse
        try {
            java.lang.reflect.Method method = TransferRequestService.class.getDeclaredMethod(
                "deductFromWarehouseInventory", UUID.class, UUID.class, Integer.class, TransferRequest.class);
            method.setAccessible(true);
            method.invoke(transferRequestService, testWarehouse.getId(), testProduct.getId(), 
                testTransfer.getApprovedQuantity(), testTransfer);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        // Then - verify warehouse inventory was deducted
        verify(warehouseInventoryRepository, times(1)).save(warehouseInventory);
        assertEquals(100, warehouseInventory.getCurrentQuantity(), 
            "Warehouse inventory should be decreased by shipped quantity");

        // Verify warehouse repository was called to get warehouse for logging
        verify(warehouseRepository, times(1)).findById(testWarehouse.getId());
        // Verify product repository was called to get product for logging
        verify(productRepository, times(1)).findById(testProduct.getId());
        
        // ✅ NEW: Verify warehouse withdrawal record was created
        assertNotNull(capturedWithdrawal[0], "Warehouse withdrawal record should be created");
        assertEquals(testWarehouse, capturedWithdrawal[0].getWarehouse(), 
            "Withdrawal record should be for the correct warehouse");
        assertEquals(testProduct, capturedWithdrawal[0].getProduct(), 
            "Withdrawal record should be for the correct product");
        assertEquals(testTransfer.getApprovedQuantity(), capturedWithdrawal[0].getQuantity(), 
            "Withdrawal record should have the correct quantity");
        assertEquals(WarehouseInventoryWithdrawal.TransactionType.TRANSFER_OUT, 
            capturedWithdrawal[0].getTransactionType(), 
            "Withdrawal record should be marked as TRANSFER_OUT");
        assertTrue(capturedWithdrawal[0].getReferenceNumber().startsWith("TRANSFER-"), 
            "Reference number should start with TRANSFER-");
        assertTrue(capturedWithdrawal[0].getNotes().contains("Outbound transfer"), 
            "Notes should mention outbound transfer");
        assertTrue(capturedWithdrawal[0].getNotes().contains(testStore.getStoreName()), 
            "Notes should include destination store name");
        assertEquals(testStore.getStoreName(), capturedWithdrawal[0].getDestination(), 
            "Destination should be the store name");
        assertEquals(approvedUser.getUsername(), capturedWithdrawal[0].getCreatedBy(), 
            "Created by should be the approving user");
        assertEquals(WarehouseInventoryWithdrawal.TransactionStatus.COMPLETED, 
            capturedWithdrawal[0].getStatus(), 
            "Status should be COMPLETED");
        assertEquals(LocalDate.now(), capturedWithdrawal[0].getWithdrawalDate(), 
            "Withdrawal date should be today");
        
        // Verify withdrawal record was saved
        verify(withdrawalRepository, times(1)).save(any(WarehouseInventoryWithdrawal.class));
    }

    @Test
    public void testTransferWithDamagedItems() {
        // Given - transfer with damaged items
        testTransfer.setReceivedQuantity(95);
        testTransfer.setDamagedQuantity(5);

        WarehouseInventory warehouseInventory = new WarehouseInventory();
        warehouseInventory.setWarehouse(testWarehouse);
        warehouseInventory.setProduct(testProduct);
        warehouseInventory.setCurrentQuantity(200);

        lenient().when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        // Mock findBySkuAndStoreId to return the existing store product
        lenient().when(productRepository.findBySkuAndStoreId(testProduct.getSku(), testStore.getId()))
            .thenReturn(Optional.of(testProduct));
        lenient().when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));
        lenient().when(warehouseRepository.findById(testWarehouse.getId()))
            .thenReturn(Optional.of(testWarehouse));
        lenient().when(storeRepository.findById(testStore.getId()))
            .thenReturn(Optional.of(testStore));

        StoreInventoryAddition[] capturedAddition = new StoreInventoryAddition[1];
        when(additionRepository.save(any(StoreInventoryAddition.class)))
            .thenAnswer(invocation -> {
                capturedAddition[0] = invocation.getArgument(0);
                return capturedAddition[0];
            });

        // When - only good quantity should be added to store (received - damaged)
        int goodQuantity = 90; // 95 - 5
        try {
            java.lang.reflect.Method method = TransferRequestService.class.getDeclaredMethod(
                "addToStoreInventory", UUID.class, Integer.class, TransferRequest.class);
            method.setAccessible(true);
            method.invoke(transferRequestService, testProduct.getId(), goodQuantity, testTransfer);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        // Then - restock record should only include good quantity
        assertNotNull(capturedAddition[0], "Restock history record should be created");
        assertEquals(90, capturedAddition[0].getQuantity(), 
            "Restock record should only include good items (excluding damaged)");
        assertEquals(140, testProduct.getQuantity(), 
            "Product quantity should only increase by good quantity (50 + 90)");
    }
}
