package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryAdditionRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryWithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for role-based access control and pagination in warehouse inventory
 */
public class WarehouseInventoryRBACTest {

    @Mock
    private WarehouseInventoryAdditionRepository additionRepository;

    @Mock
    private WarehouseInventoryWithdrawalRepository withdrawalRepository;

    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;

    @InjectMocks
    private WarehouseInventoryService warehouseInventoryService;

    private UUID warehouseId;
    private User gmUser;
    private User employeeUser;
    private Pageable pageable;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        warehouseId = UUID.randomUUID();
        
        // Create GM user
        gmUser = new User();
        gmUser.setId(UUID.randomUUID());
        gmUser.setUsername("gm_user");
        
        // Create Employee user
        employeeUser = new User();
        employeeUser.setId(UUID.randomUUID());
        employeeUser.setUsername("employee_user");
        
        // Setup pageable
        pageable = PageRequest.of(0, 20);
    }

    @Test
    public void testGetUserCompanyRole_ReturnsFounder() {
        // Setup company membership with FOUNDER role
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.FOUNDER);
        membership.setIsActive(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(gmUser))
            .thenReturn(memberships);
        
        // Test
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(gmUser);
        
        // Verify
        assertEquals(CompanyRole.FOUNDER, role);
        verify(companyStoreUserRepository, times(1)).findByUserAndIsActiveTrue(gmUser);
        
        System.out.println("✅ Test passed: getUserCompanyRole returns FOUNDER correctly");
    }

    @Test
    public void testGetUserCompanyRole_ReturnsCEO() {
        // Setup company membership with CEO role
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.CEO);
        membership.setIsActive(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(gmUser))
            .thenReturn(memberships);
        
        // Test
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(gmUser);
        
        // Verify
        assertEquals(CompanyRole.CEO, role);
        verify(companyStoreUserRepository, times(1)).findByUserAndIsActiveTrue(gmUser);
        
        System.out.println("✅ Test passed: getUserCompanyRole returns CEO correctly");
    }

    @Test
    public void testGetUserCompanyRole_ReturnsGeneralManager() {
        // Setup company membership with GENERAL_MANAGER role
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.GENERAL_MANAGER);
        membership.setIsActive(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(gmUser))
            .thenReturn(memberships);
        
        // Test
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(gmUser);
        
        // Verify
        assertEquals(CompanyRole.GENERAL_MANAGER, role);
        verify(companyStoreUserRepository, times(1)).findByUserAndIsActiveTrue(gmUser);
        
        System.out.println("✅ Test passed: getUserCompanyRole returns GENERAL_MANAGER correctly");
    }

    @Test
    public void testGetUserCompanyRole_ReturnsEmployee() {
        // Setup company membership with EMPLOYEE role
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setRole(CompanyRole.EMPLOYEE);
        membership.setIsActive(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(employeeUser))
            .thenReturn(memberships);
        
        // Test
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(employeeUser);
        
        // Verify
        assertEquals(CompanyRole.EMPLOYEE, role);
        verify(companyStoreUserRepository, times(1)).findByUserAndIsActiveTrue(employeeUser);
        
        System.out.println("✅ Test passed: getUserCompanyRole returns EMPLOYEE correctly");
    }

    @Test
    public void testGetUserCompanyRole_NoMembership_ReturnsEmployee() {
        // Setup empty memberships
        List<CompanyStoreUser> memberships = new ArrayList<>();
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(employeeUser))
            .thenReturn(memberships);
        
        // Test
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(employeeUser);
        
        // Verify - should default to EMPLOYEE
        assertEquals(CompanyRole.EMPLOYEE, role);
        verify(companyStoreUserRepository, times(1)).findByUserAndIsActiveTrue(employeeUser);
        
        System.out.println("✅ Test passed: getUserCompanyRole defaults to EMPLOYEE when no membership");
    }

    @Test
    public void testListAdditions_WithPagination_NoUserFilter() {
        // Setup mock additions
        List<WarehouseInventoryAddition> additions = createMockAdditions(5);
        Page<WarehouseInventoryAddition> additionsPage = new PageImpl<>(additions, pageable, 5);
        
        when(additionRepository.findByWarehouseId(eq(warehouseId), any(Pageable.class)))
            .thenReturn(additionsPage);
        
        // Test - GM+ user (no filter)
        Page<WarehouseInventoryAddition> result = warehouseInventoryService.listAdditions(
            warehouseId, null, null, null, null, pageable
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertFalse(result.hasNext());
        verify(additionRepository, times(1)).findByWarehouseId(eq(warehouseId), any(Pageable.class));
        verify(additionRepository, never()).findByWarehouseIdAndCreatedBy(any(), any(), any());
        
        System.out.println("✅ Test passed: listAdditions with pagination and no user filter");
    }

    @Test
    public void testListAdditions_WithPagination_WithUserFilter() {
        // Setup mock additions
        List<WarehouseInventoryAddition> additions = createMockAdditions(3);
        Page<WarehouseInventoryAddition> additionsPage = new PageImpl<>(additions, pageable, 3);
        
        when(additionRepository.findByWarehouseIdAndCreatedBy(eq(warehouseId), eq("employee_user"), any(Pageable.class)))
            .thenReturn(additionsPage);
        
        // Test - Employee user (with filter)
        Page<WarehouseInventoryAddition> result = warehouseInventoryService.listAdditions(
            warehouseId, null, null, null, "employee_user", pageable
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertFalse(result.hasNext());
        verify(additionRepository, times(1)).findByWarehouseIdAndCreatedBy(eq(warehouseId), eq("employee_user"), any(Pageable.class));
        verify(additionRepository, never()).findByWarehouseId(any(), any());
        
        System.out.println("✅ Test passed: listAdditions with pagination and user filter");
    }

    @Test
    public void testListWithdrawals_WithPagination_NoUserFilter() {
        // Setup mock withdrawals
        List<WarehouseInventoryWithdrawal> withdrawals = createMockWithdrawals(5);
        Page<WarehouseInventoryWithdrawal> withdrawalsPage = new PageImpl<>(withdrawals, pageable, 5);
        
        when(withdrawalRepository.findByWarehouseId(eq(warehouseId), any(Pageable.class)))
            .thenReturn(withdrawalsPage);
        
        // Test - GM+ user (no filter)
        Page<WarehouseInventoryWithdrawal> result = warehouseInventoryService.listWithdrawals(
            warehouseId, null, null, null, null, pageable
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertFalse(result.hasNext());
        verify(withdrawalRepository, times(1)).findByWarehouseId(eq(warehouseId), any(Pageable.class));
        verify(withdrawalRepository, never()).findByWarehouseIdAndCreatedBy(any(), any(), any());
        
        System.out.println("✅ Test passed: listWithdrawals with pagination and no user filter");
    }

    @Test
    public void testListWithdrawals_WithPagination_WithUserFilter() {
        // Setup mock withdrawals
        List<WarehouseInventoryWithdrawal> withdrawals = createMockWithdrawals(3);
        Page<WarehouseInventoryWithdrawal> withdrawalsPage = new PageImpl<>(withdrawals, pageable, 3);
        
        when(withdrawalRepository.findByWarehouseIdAndCreatedBy(eq(warehouseId), eq("employee_user"), any(Pageable.class)))
            .thenReturn(withdrawalsPage);
        
        // Test - Employee user (with filter)
        Page<WarehouseInventoryWithdrawal> result = warehouseInventoryService.listWithdrawals(
            warehouseId, null, null, null, "employee_user", pageable
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertFalse(result.hasNext());
        verify(withdrawalRepository, times(1)).findByWarehouseIdAndCreatedBy(eq(warehouseId), eq("employee_user"), any(Pageable.class));
        verify(withdrawalRepository, never()).findByWarehouseId(any(), any());
        
        System.out.println("✅ Test passed: listWithdrawals with pagination and user filter");
    }

    @Test
    public void testPagination_HasMore() {
        // Setup mock additions with more pages available
        List<WarehouseInventoryAddition> additions = createMockAdditions(20);
        Page<WarehouseInventoryAddition> additionsPage = new PageImpl<>(additions, pageable, 50); // Total 50 items
        
        when(additionRepository.findByWarehouseId(eq(warehouseId), any(Pageable.class)))
            .thenReturn(additionsPage);
        
        // Test
        Page<WarehouseInventoryAddition> result = warehouseInventoryService.listAdditions(
            warehouseId, null, null, null, null, pageable
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(20, result.getContent().size());
        assertEquals(50, result.getTotalElements());
        assertTrue(result.hasNext()); // Should have more pages
        assertEquals(3, result.getTotalPages()); // 50 items / 20 per page = 3 pages
        
        System.out.println("✅ Test passed: pagination indicates hasMore correctly");
    }

    /**
     * Helper method to create mock additions
     */
    private List<WarehouseInventoryAddition> createMockAdditions(int count) {
        List<WarehouseInventoryAddition> additions = new ArrayList<>();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        
        for (int i = 0; i < count; i++) {
            WarehouseInventoryAddition addition = new WarehouseInventoryAddition(warehouse, product, 10);
            addition.setId(UUID.randomUUID());
            addition.setCreatedBy("test_user_" + i);
            addition.setReceiptDate(LocalDate.now());
            additions.add(addition);
        }
        
        return additions;
    }

    /**
     * Helper method to create mock withdrawals
     */
    private List<WarehouseInventoryWithdrawal> createMockWithdrawals(int count) {
        List<WarehouseInventoryWithdrawal> withdrawals = new ArrayList<>();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        
        for (int i = 0; i < count; i++) {
            WarehouseInventoryWithdrawal withdrawal = new WarehouseInventoryWithdrawal(
                warehouse, product, 5, "Test reason"
            );
            withdrawal.setId(UUID.randomUUID());
            withdrawal.setCreatedBy("test_user_" + i);
            withdrawal.setWithdrawalDate(LocalDate.now());
            withdrawals.add(withdrawal);
        }
        
        return withdrawals;
    }
}
