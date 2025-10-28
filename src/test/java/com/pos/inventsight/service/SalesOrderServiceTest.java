package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SalesOrderService
 */
public class SalesOrderServiceTest {
    
    @Mock
    private SalesOrderRepository salesOrderRepository;
    
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    
    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;
    
    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private SyncChangeService syncChangeService;
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private SalesOrderService salesOrderService;
    
    private UUID tenantId;
    private UUID warehouseId;
    private UUID productId;
    private Warehouse warehouse;
    private Product product;
    private WarehouseInventory inventory;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test data
        tenantId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        warehouse = new Warehouse("Test Warehouse", "Test Location");
        warehouse.setId(warehouseId);
        
        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setSku("TEST-001");
        product.setRetailPrice(new BigDecimal("100.00"));
        
        inventory = new WarehouseInventory(warehouse, product, 100);
        inventory.setId(UUID.randomUUID());
        inventory.setReservedQuantity(0);
        
        // Set configuration values
        ReflectionTestUtils.setField(salesOrderService, "salesEnabled", true);
        ReflectionTestUtils.setField(salesOrderService, "maxEmployeeDiscountPercent", 10.0);
        ReflectionTestUtils.setField(salesOrderService, "crossStoreRequiresApproval", true);
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
    }
    
    @Test
    void testCreateOrder_Success() {
        // Arrange
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(UUID.randomUUID());
        
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.createOrder(tenantId, "USD", "John Doe", "123-456-7890", authentication);
        
        // Assert
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        verify(salesOrderRepository, times(1)).save(any(SalesOrder.class));
        verify(syncChangeService, times(1)).recordChange(eq("SalesOrder"), anyString(), eq("INSERT"), any());
    }
    
    @Test
    void testAddItem_Success_WithReservation() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenReturn(inventory);
        when(salesOrderItemRepository.save(any(SalesOrderItem.class)))
            .thenAnswer(invocation -> {
                SalesOrderItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // Act
        SalesOrderItem result = salesOrderService.addItem(
            orderId, warehouseId, productId, 10, 
            new BigDecimal("100.00"), BigDecimal.ZERO, "USD", false
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(10, result.getQuantity());
        assertEquals(new BigDecimal("100.00"), result.getUnitPrice());
        assertEquals(10, inventory.getReservedQuantity()); // Stock should be reserved
        verify(warehouseInventoryRepository, times(1)).save(any(WarehouseInventory.class));
        verify(syncChangeService, times(2)).recordChange(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void testAddItem_InsufficientStock_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        inventory.setCurrentQuantity(5); // Only 5 units available
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            salesOrderService.addItem(
                orderId, warehouseId, productId, 10, // Requesting 10 units
                new BigDecimal("100.00"), BigDecimal.ZERO, "USD", false
            );
        });
        
        verify(warehouseInventoryRepository, never()).save(any(WarehouseInventory.class));
    }
    
    @Test
    void testAddItem_HighDiscount_RequiresManagerApproval() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenReturn(inventory);
        when(salesOrderItemRepository.save(any(SalesOrderItem.class)))
            .thenAnswer(invocation -> {
                SalesOrderItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act - Employee applying 15% discount (exceeds 10% threshold)
        salesOrderService.addItem(
            orderId, warehouseId, productId, 10,
            new BigDecimal("100.00"), new BigDecimal("15.00"), "USD", true // isEmployee=true
        );
        
        // Assert
        assertTrue(order.getRequiresManagerApproval());
        verify(salesOrderRepository, times(1)).save(order);
    }
    
    @Test
    void testAddItem_CrossStore_RequiresManagerApproval() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        // Existing item from different warehouse
        UUID otherWarehouseId = UUID.randomUUID();
        Warehouse otherWarehouse = new Warehouse("Other Warehouse", "Other Location");
        otherWarehouse.setId(otherWarehouseId);
        SalesOrderItem existingItem = new SalesOrderItem(order, otherWarehouse, product, 5, new BigDecimal("100.00"), "USD");
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenReturn(inventory);
        when(salesOrderItemRepository.save(any(SalesOrderItem.class)))
            .thenAnswer(invocation -> {
                SalesOrderItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(existingItem));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act - Employee adding item from different warehouse
        salesOrderService.addItem(
            orderId, warehouseId, productId, 10,
            new BigDecimal("100.00"), BigDecimal.ZERO, "USD", true // isEmployee=true
        );
        
        // Assert - Cross-store sourcing should require manager approval
        assertTrue(order.getRequiresManagerApproval());
        verify(salesOrderRepository, times(1)).save(order);
    }
    
    @Test
    void testSubmit_WithoutApproval_StatusConfirmed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        order.setRequiresManagerApproval(false);
        
        SalesOrderItem item = new SalesOrderItem(order, warehouse, product, 5, new BigDecimal("100.00"), "USD");
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(item));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.submit(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(salesOrderRepository, times(1)).save(order);
        verify(syncChangeService, times(1)).recordChange(eq("SalesOrder"), anyString(), eq("UPDATE"), any());
    }
    
    @Test
    void testSubmit_WithApproval_StatusPendingManagerApproval() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        order.setRequiresManagerApproval(true);
        
        SalesOrderItem item = new SalesOrderItem(order, warehouse, product, 5, new BigDecimal("100.00"), "USD");
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(item));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.submit(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.PENDING_MANAGER_APPROVAL, result.getStatus());
        verify(salesOrderRepository, times(1)).save(order);
    }
    
    @Test
    void testSubmit_EmptyOrder_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            salesOrderService.submit(orderId, authentication);
        });
    }
    
    @Test
    void testRequestCancel_DraftOrder_CancelsImmediately() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.requestCancel(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(salesOrderRepository, times(1)).save(order);
        verify(syncChangeService, times(1)).recordChange(eq("SalesOrder"), anyString(), eq("UPDATE"), any());
    }
    
    @Test
    void testRequestCancel_ConfirmedOrder_RequiresApproval() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.requestCancel(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.CANCEL_REQUESTED, result.getStatus());
        verify(salesOrderRepository, times(1)).save(order);
    }
    
    @Test
    void testRequestCancel_ReleasesReservations() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.DRAFT);
        
        SalesOrderItem item = new SalesOrderItem(order, warehouse, product, 10, new BigDecimal("100.00"), "USD");
        inventory.setReservedQuantity(10);
        
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(item));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        salesOrderService.requestCancel(orderId, authentication);
        
        // Assert
        assertEquals(0, inventory.getReservedQuantity()); // Reservations should be released
        verify(warehouseInventoryRepository, times(1)).save(inventory);
    }
    
    @Test
    void testApprove_PendingOrder_MovesToConfirmed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_MANAGER_APPROVAL);
        
        User mockUser = new User();
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.STORE_MANAGER);
        
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
            .thenReturn(Arrays.asList(membership));
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.approve(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(salesOrderRepository, times(1)).save(order);
        verify(syncChangeService, times(1)).recordChange(eq("SalesOrder"), anyString(), eq("UPDATE"), any());
    }
    
    @Test
    void testApproveCancel_ReleasesReservations() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, "USD", "testuser");
        order.setId(orderId);
        order.setStatus(OrderStatus.CANCEL_REQUESTED);
        
        SalesOrderItem item = new SalesOrderItem(order, warehouse, product, 10, new BigDecimal("100.00"), "USD");
        inventory.setReservedQuantity(10);
        
        User mockUser = new User();
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.STORE_MANAGER);
        
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
            .thenReturn(Arrays.asList(membership));
        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(salesOrderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(item));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductIdWithLock(warehouseId, productId))
            .thenReturn(Optional.of(inventory));
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);
        
        // Act
        SalesOrder result = salesOrderService.approveCancel(orderId, authentication);
        
        // Assert
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(0, inventory.getReservedQuantity());
        verify(warehouseInventoryRepository, times(1)).save(inventory);
        verify(salesOrderRepository, times(1)).save(order);
    }
}
