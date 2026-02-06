package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Transfer Request Inventory Deduction Timing
 * Tests that inventory is deducted when transfer moves to IN_TRANSIT status,
 * not when it's COMPLETED.
 */
@ExtendWith(MockitoExtension.class)
public class TransferRequestInventoryDeductionTimingTest {

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

    @InjectMocks
    private TransferRequestService transferRequestService;

    private TransferRequest testTransfer;
    private Product testProduct;
    private Store testStore;
    private Warehouse testWarehouse;
    private User testUser;
    private Company testCompany;
    private WarehouseInventory warehouseInventory;

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

        // Setup warehouse inventory with 200 units
        warehouseInventory = new WarehouseInventory();
        warehouseInventory.setWarehouse(testWarehouse);
        warehouseInventory.setProduct(testProduct);
        warehouseInventory.setCurrentQuantity(200);

        // Setup test transfer (from warehouse to store)
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
        testTransfer.setStatus(TransferRequestStatus.READY);
    }

    @Test
    public void testPickupTransfer_DeductsWarehouseInventoryImmediately() {
        // Given - transfer is READY for pickup
        when(transferRequestRepository.findById(testTransfer.getId()))
            .thenReturn(Optional.of(testTransfer));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));
        when(warehouseRepository.findById(testWarehouse.getId()))
            .thenReturn(Optional.of(testWarehouse));
        when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        when(transferRequestRepository.save(any(TransferRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - pickup transfer (READY → IN_TRANSIT)
        TransferRequest result = transferRequestService.pickupTransfer(
            testTransfer.getId(),
            "John Carrier",
            "+1234567890",
            "VAN-123",
            LocalDateTime.now().plusDays(1),
            "QR-CODE-DATA",
            testUser
        );

        // Then - warehouse inventory should be deducted immediately
        verify(warehouseInventoryRepository, times(1)).save(warehouseInventory);
        assertEquals(100, warehouseInventory.getCurrentQuantity(), 
            "Warehouse inventory should be deducted by 100 units (200 - 100)");
        
        // And transfer status should be IN_TRANSIT
        assertEquals(TransferRequestStatus.IN_TRANSIT, result.getStatus(),
            "Transfer should be marked as IN_TRANSIT");
        
        // And warehouse repository should be called for logging
        verify(warehouseRepository, times(1)).findById(testWarehouse.getId());
        verify(productRepository, times(1)).findById(testProduct.getId());
    }

    @Test
    public void testApproveAndSend_DeductsWarehouseInventoryImmediately() {
        // Given - transfer is PENDING
        testTransfer.setStatus(TransferRequestStatus.PENDING);
        testTransfer.setApprovedQuantity(null); // Not yet approved
        
        when(transferRequestRepository.findById(testTransfer.getId()))
            .thenReturn(Optional.of(testTransfer));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));
        when(warehouseRepository.findById(testWarehouse.getId()))
            .thenReturn(Optional.of(testWarehouse));
        when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        when(transferRequestRepository.save(any(TransferRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - approve and send (direct PENDING → IN_TRANSIT)
        TransferRequest result = transferRequestService.approveAndSend(
            testTransfer.getId(),
            80, // Approve 80 out of 100 requested
            "Jane Carrier",
            "+0987654321",
            "TRUCK-456",
            LocalDateTime.now().plusDays(2),
            "Expedited delivery",
            testUser
        );

        // Then - warehouse inventory should be deducted immediately by approved quantity
        verify(warehouseInventoryRepository, times(1)).save(warehouseInventory);
        assertEquals(120, warehouseInventory.getCurrentQuantity(), 
            "Warehouse inventory should be deducted by 80 units (200 - 80)");
        
        // And transfer status should be IN_TRANSIT
        assertEquals(TransferRequestStatus.IN_TRANSIT, result.getStatus(),
            "Transfer should be marked as IN_TRANSIT");
        
        // And approved quantity should be set
        assertEquals(80, result.getApprovedQuantity(),
            "Approved quantity should be 80");
    }

    @Test
    public void testPickupTransfer_FromStore_DeductsStoreInventory() {
        // Given - transfer from store to warehouse
        testTransfer.setFromLocationType("STORE");
        testTransfer.setFromLocationId(testStore.getId());
        testTransfer.setToLocationType("WAREHOUSE");
        testTransfer.setToLocationId(testWarehouse.getId());
        testProduct.setQuantity(150); // Store has 150 units
        
        when(transferRequestRepository.findById(testTransfer.getId()))
            .thenReturn(Optional.of(testTransfer));
        when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        when(transferRequestRepository.save(any(TransferRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - pickup transfer from store
        TransferRequest result = transferRequestService.pickupTransfer(
            testTransfer.getId(),
            "Bob Carrier",
            "+1122334455",
            "BIKE-789",
            LocalDateTime.now().plusHours(5),
            "QR-STORE-CODE",
            testUser
        );

        // Then - store inventory (product quantity) should be deducted
        verify(productRepository, times(1)).save(testProduct);
        assertEquals(50, testProduct.getQuantity(), 
            "Store inventory should be deducted by 100 units (150 - 100)");
        
        // And transfer status should be IN_TRANSIT
        assertEquals(TransferRequestStatus.IN_TRANSIT, result.getStatus(),
            "Transfer should be marked as IN_TRANSIT");
    }

    @Test
    public void testPickupTransfer_ThrowsExceptionIfNotReady() {
        // Given - transfer is not in READY status
        testTransfer.setStatus(TransferRequestStatus.PENDING);
        
        when(transferRequestRepository.findById(testTransfer.getId()))
            .thenReturn(Optional.of(testTransfer));

        // When/Then - should throw exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            transferRequestService.pickupTransfer(
                testTransfer.getId(),
                "Carrier",
                "+1234567890",
                "VAN",
                LocalDateTime.now().plusDays(1),
                "QR",
                testUser
            );
        });
        
        assertTrue(exception.getMessage().contains("Only READY transfers can be picked up"),
            "Exception message should indicate only READY transfers can be picked up");
        
        // And inventory should NOT be deducted
        verify(warehouseInventoryRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    public void testPickupTransfer_ThrowsExceptionIfInsufficientInventory() {
        // Given - warehouse has insufficient inventory
        warehouseInventory.setCurrentQuantity(50); // Only 50 units, but need 100
        
        when(transferRequestRepository.findById(testTransfer.getId()))
            .thenReturn(Optional.of(testTransfer));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            testWarehouse.getId(), testProduct.getId()))
            .thenReturn(Optional.of(warehouseInventory));

        // When/Then - should throw exception about insufficient inventory
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            transferRequestService.pickupTransfer(
                testTransfer.getId(),
                "Carrier",
                "+1234567890",
                "VAN",
                LocalDateTime.now().plusDays(1),
                "QR",
                testUser
            );
        });
        
        assertTrue(exception.getMessage().contains("Insufficient inventory"),
            "Exception message should indicate insufficient inventory");
        assertTrue(exception.getMessage().contains("Available: 50"),
            "Exception should show available quantity");
        assertTrue(exception.getMessage().contains("Required: 100"),
            "Exception should show required quantity");
    }

    @Test
    public void testCompleteTransfer_OnlyAddsToDestination_DoesNotDeductFromSource() {
        // Given - transfer is IN_TRANSIT (inventory already deducted)
        testTransfer.setStatus(TransferRequestStatus.IN_TRANSIT);
        testTransfer.setReceivedQuantity(95); // Received 95 out of 100
        testTransfer.setDamagedQuantity(5); // 5 damaged
        testTransfer.setReceivedByUser(testUser);
        
        // Reset warehouse inventory to simulate it was already deducted at IN_TRANSIT
        warehouseInventory.setCurrentQuantity(100); // Already deducted 100 units
        
        when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));
        when(additionRepository.save(any(StoreInventoryAddition.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - complete transfer (adds to destination only)
        try {
            java.lang.reflect.Method method = TransferRequestService.class.getDeclaredMethod(
                "updateInventoryForTransferCompletion", TransferRequest.class);
            method.setAccessible(true);
            method.invoke(transferRequestService, testTransfer);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        // Then - should only add good items to destination (90 = 95 - 5)
        verify(productRepository, times(1)).save(testProduct);
        assertEquals(140, testProduct.getQuantity(), 
            "Store should receive 90 good units (50 original + 90 good units from 95 received - 5 damaged)");
        
        // And should create restock record
        verify(additionRepository, times(1)).save(any(StoreInventoryAddition.class));
        
        // And should NOT deduct from warehouse again (already deducted at IN_TRANSIT)
        verify(warehouseInventoryRepository, never()).save(any());
        
        // Warehouse inventory should remain at 100 (not deducted again)
        assertEquals(100, warehouseInventory.getCurrentQuantity(),
            "Warehouse inventory should remain at 100 (already deducted at IN_TRANSIT, not deducted again)");
    }

    @Test
    public void testCompleteTransfer_WarehouseToWarehouse_OnlyAddsToDestination() {
        // Given - warehouse to warehouse transfer
        testTransfer.setToLocationType("WAREHOUSE");
        UUID destinationWarehouseId = UUID.randomUUID();
        testTransfer.setToLocationId(destinationWarehouseId);
        testTransfer.setStatus(TransferRequestStatus.IN_TRANSIT);
        testTransfer.setReceivedQuantity(100);
        testTransfer.setDamagedQuantity(0);
        
        // Destination warehouse inventory
        WarehouseInventory destInventory = new WarehouseInventory();
        destInventory.setCurrentQuantity(50);
        
        Warehouse destWarehouse = new Warehouse();
        destWarehouse.setId(destinationWarehouseId);
        destWarehouse.setName("Destination Warehouse");
        
        // Use lenient() because updateInventoryForTransferCompletion() only adds to destination
        // and doesn't query source warehouse (already deducted at IN_TRANSIT)
        lenient().when(warehouseInventoryRepository.findByWarehouseIdAndProductId(
            destinationWarehouseId, testProduct.getId()))
            .thenReturn(Optional.of(destInventory));
        lenient().when(warehouseRepository.findById(destinationWarehouseId))
            .thenReturn(Optional.of(destWarehouse));
        lenient().when(productRepository.findById(testProduct.getId()))
            .thenReturn(Optional.of(testProduct));

        // When - complete transfer
        try {
            java.lang.reflect.Method method = TransferRequestService.class.getDeclaredMethod(
                "updateInventoryForTransferCompletion", TransferRequest.class);
            method.setAccessible(true);
            method.invoke(transferRequestService, testTransfer);
        } catch (Exception e) {
            fail("Failed to invoke private method: " + e.getMessage());
        }

        // Then - should only add to destination warehouse
        verify(warehouseInventoryRepository, times(1)).save(destInventory);
        assertEquals(150, destInventory.getCurrentQuantity(), 
            "Destination warehouse should receive 100 units (50 + 100)");
        
        // And should NOT deduct from source warehouse (already deducted at IN_TRANSIT)
        // Only one save call should be for destination, not source
        verify(warehouseInventoryRepository, times(1)).save(any());
    }
}
