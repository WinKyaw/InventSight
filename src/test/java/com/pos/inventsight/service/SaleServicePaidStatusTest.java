package com.pos.inventsight.service;

import com.pos.inventsight.dto.SaleResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for verifying the PAID status flow and pickup completion in SaleService
 */
@ExtendWith(MockitoExtension.class)
public class SaleServicePaidStatusTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private ProductService productService;

    @Mock
    private InventoryAnalyticsService inventoryAnalyticsService;

    @Mock
    private com.pos.inventsight.repository.sql.CustomerRepository customerRepository;

    @InjectMocks
    private SaleService saleService;

    private Sale testSale;
    private User testUser;
    private Company testCompany;
    private Store testStore;

    @BeforeEach
    public void setUp() {
        // Create test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");

        // Create test store
        testStore = new Store();
        testStore.setId(UUID.randomUUID());
        testStore.setStoreName("Test Store");
        testStore.setCompany(testCompany);

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setDefaultTenantId(testCompany.getId());

        // Create test sale
        testSale = new Sale();
        testSale.setId(1L);
        testSale.setReceiptNumber("RCP-TEST-001");
        testSale.setCompany(testCompany);
        testSale.setStore(testStore);
        testSale.setProcessedBy(testUser);
        testSale.setTotalAmount(new BigDecimal("100.00"));
        testSale.setStatus(SaleStatus.PENDING);
        testSale.setItems(new ArrayList<>());
    }

    /**
     * Test that completeReceipt sets status to PAID instead of COMPLETED
     */
    @Test
    public void testCompleteReceipt_SetsStatusToPaid() {
        // Given
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SaleResponse response = saleService.completeReceipt(testSale.getId(), PaymentMethod.CASH, testUser.getId());
        
        // Then
        assertNotNull(response);
        assertEquals(SaleStatus.PAID, response.getStatus());
        verify(saleRepository).save(argThat(sale -> 
            sale.getStatus() == SaleStatus.PAID && 
            sale.getPaymentMethod() == PaymentMethod.CASH
        ));
    }

    /**
     * Test that fulfillReceipt requires PAID status
     */
    @Test
    public void testFulfillReceipt_RequiresPaidStatus() {
        // Given - sale is in PENDING status
        testSale.setStatus(SaleStatus.PENDING);
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            saleService.fulfillReceipt(testSale.getId(), ReceiptType.PICKUP, testUser.getId());
        });
        
        assertTrue(exception.getMessage().contains("Receipt must be paid before fulfillment"));
        assertTrue(exception.getMessage().contains("PENDING"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    /**
     * Test that fulfillReceipt succeeds when status is PAID
     */
    @Test
    public void testFulfillReceipt_SucceedsWithPaidStatus() {
        // Given - sale is in PAID status
        testSale.setStatus(SaleStatus.PAID);
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SaleResponse response = saleService.fulfillReceipt(testSale.getId(), ReceiptType.PICKUP, testUser.getId());
        
        // Then
        assertNotNull(response);
        assertEquals(SaleStatus.READY_FOR_PICKUP, response.getStatus());
        assertEquals(ReceiptType.PICKUP, response.getReceiptType());
        verify(saleRepository).save(argThat(sale -> 
            sale.getStatus() == SaleStatus.READY_FOR_PICKUP &&
            sale.getFulfilledBy() == testUser
        ));
    }

    /**
     * Test markAsPickedUp with valid PICKUP receipt in READY_FOR_PICKUP status
     */
    @Test
    public void testMarkAsPickedUp_Success() {
        // Given
        testSale.setStatus(SaleStatus.READY_FOR_PICKUP);
        testSale.setReceiptType(ReceiptType.PICKUP);
        
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SaleResponse response = saleService.markAsPickedUp(testSale.getId(), testUser.getId());
        
        // Then
        assertNotNull(response);
        assertEquals(SaleStatus.COMPLETED, response.getStatus());
        verify(saleRepository).save(argThat(sale -> 
            sale.getStatus() == SaleStatus.COMPLETED
        ));
        verify(activityLogService).logActivity(
            eq(testUser.getId().toString()),
            eq(testUser.getUsername()),
            eq("SALE_PICKED_UP"),
            eq("SALE"),
            contains("picked up and completed")
        );
    }

    /**
     * Test markAsPickedUp fails with non-PICKUP receipt type
     */
    @Test
    public void testMarkAsPickedUp_FailsWithNonPickupType() {
        // Given
        testSale.setStatus(SaleStatus.READY_FOR_PICKUP);
        testSale.setReceiptType(ReceiptType.DELIVERY);
        
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            saleService.markAsPickedUp(testSale.getId(), testUser.getId());
        });
        
        assertTrue(exception.getMessage().contains("Cannot mark non-pickup receipt as picked up"));
        assertTrue(exception.getMessage().contains("DELIVERY"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    /**
     * Test markAsPickedUp fails when receipt is not in READY_FOR_PICKUP status
     */
    @Test
    public void testMarkAsPickedUp_FailsWithWrongStatus() {
        // Given
        testSale.setStatus(SaleStatus.PAID);
        testSale.setReceiptType(ReceiptType.PICKUP);
        
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            saleService.markAsPickedUp(testSale.getId(), testUser.getId());
        });
        
        assertTrue(exception.getMessage().contains("Receipt is not ready for pickup"));
        assertTrue(exception.getMessage().contains("PAID"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    /**
     * Test markAsPickedUp fails when receipt not found
     */
    @Test
    public void testMarkAsPickedUp_NotFound() {
        // Given
        Long nonExistentId = 999L;
        when(saleRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            saleService.markAsPickedUp(nonExistentId, testUser.getId());
        });
        
        assertTrue(exception.getMessage().contains("Receipt not found"));
        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
    }

    /**
     * Test complete workflow: PENDING -> PAID -> READY_FOR_PICKUP -> COMPLETED
     */
    @Test
    public void testCompletePickupWorkflow() {
        // Step 1: Complete payment (PENDING -> PAID)
        testSale.setStatus(SaleStatus.PENDING);
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale savedSale = invocation.getArgument(0);
            testSale.setStatus(savedSale.getStatus());
            testSale.setPaymentMethod(savedSale.getPaymentMethod());
            return savedSale;
        });
        
        SaleResponse paymentResponse = saleService.completeReceipt(testSale.getId(), PaymentMethod.CASH, testUser.getId());
        assertEquals(SaleStatus.PAID, paymentResponse.getStatus());
        
        // Step 2: Fulfill for pickup (PAID -> READY_FOR_PICKUP)
        testSale.setStatus(SaleStatus.PAID);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale savedSale = invocation.getArgument(0);
            testSale.setStatus(savedSale.getStatus());
            testSale.setReceiptType(savedSale.getReceiptType());
            testSale.setFulfilledBy(savedSale.getFulfilledBy());
            return savedSale;
        });
        
        SaleResponse fulfillResponse = saleService.fulfillReceipt(testSale.getId(), ReceiptType.PICKUP, testUser.getId());
        assertEquals(SaleStatus.READY_FOR_PICKUP, fulfillResponse.getStatus());
        assertEquals(ReceiptType.PICKUP, fulfillResponse.getReceiptType());
        
        // Step 3: Mark as picked up (READY_FOR_PICKUP -> COMPLETED)
        testSale.setStatus(SaleStatus.READY_FOR_PICKUP);
        testSale.setReceiptType(ReceiptType.PICKUP);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale savedSale = invocation.getArgument(0);
            testSale.setStatus(savedSale.getStatus());
            return savedSale;
        });
        
        SaleResponse pickupResponse = saleService.markAsPickedUp(testSale.getId(), testUser.getId());
        assertEquals(SaleStatus.COMPLETED, pickupResponse.getStatus());
    }

    /**
     * Test complete delivery workflow: PENDING -> PAID -> OUT_FOR_DELIVERY -> DELIVERED
     */
    @Test
    public void testCompleteDeliveryWorkflow() {
        // Step 1: Complete payment (PENDING -> PAID)
        testSale.setStatus(SaleStatus.PENDING);
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);
        when(saleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale savedSale = invocation.getArgument(0);
            testSale.setStatus(savedSale.getStatus());
            testSale.setPaymentMethod(savedSale.getPaymentMethod());
            return savedSale;
        });
        
        SaleResponse paymentResponse = saleService.completeReceipt(testSale.getId(), PaymentMethod.CREDIT_CARD, testUser.getId());
        assertEquals(SaleStatus.PAID, paymentResponse.getStatus());
        
        // Step 2: Fulfill for delivery (PAID -> OUT_FOR_DELIVERY)
        testSale.setStatus(SaleStatus.PAID);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale savedSale = invocation.getArgument(0);
            testSale.setStatus(savedSale.getStatus());
            testSale.setReceiptType(savedSale.getReceiptType());
            testSale.setFulfilledBy(savedSale.getFulfilledBy());
            return savedSale;
        });
        
        SaleResponse fulfillResponse = saleService.fulfillReceipt(testSale.getId(), ReceiptType.DELIVERY, testUser.getId());
        assertEquals(SaleStatus.OUT_FOR_DELIVERY, fulfillResponse.getStatus());
        assertEquals(ReceiptType.DELIVERY, fulfillResponse.getReceiptType());
    }
}
