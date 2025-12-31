package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PredefinedItemsController JWT authentication extraction
 * Tests that companyId is correctly extracted from authenticated user's token
 * instead of requiring it as a query parameter
 */
@ExtendWith(MockitoExtension.class)
class PredefinedItemsControllerJwtAuthTest {
    
    @Mock
    private PredefinedItemsService predefinedItemsService;
    
    @Mock
    private SupplyManagementService supplyManagementService;
    
    @Mock
    private CompanyService companyService;
    
    @Mock
    private UserService userService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private PredefinedItemsController controller;
    
    private User user;
    private Company company;
    private UUID companyId;
    private UUID itemId;
    private PredefinedItem predefinedItem;
    private Store store;
    private Warehouse warehouse;
    
    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        
        // Setup user with defaultTenantId
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setDefaultTenantId(companyId);
        
        // Setup company
        company = new Company();
        company.setId(companyId);
        company.setName("Test Company");
        
        // Setup predefined item
        predefinedItem = new PredefinedItem();
        predefinedItem.setId(itemId);
        predefinedItem.setName("Test Item");
        predefinedItem.setCompany(company);
        
        // Setup store
        store = new Store();
        store.setId(UUID.randomUUID());
        store.setStoreName("Test Store");
        store.setCompany(company);
        
        // Setup warehouse
        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Test Warehouse");
        warehouse.setCompany(company);
    }
    
    @Test
    void testGetAssociatedStores_ExtractsCompanyIdFromJwt() {
        // Setup authentication to return user
        when(authentication.getPrincipal()).thenReturn(user);
        when(companyService.getCompany(companyId, authentication)).thenReturn(company);
        when(predefinedItemsService.getItemById(itemId, company)).thenReturn(predefinedItem);
        when(predefinedItemsService.getAssociatedStores(predefinedItem)).thenReturn(Arrays.asList(store));
        
        // Execute - no companyId parameter needed
        ResponseEntity<GenericApiResponse<List<StoreResponse>>> response = 
            controller.getAssociatedStores(itemId, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        
        // Verify that companyService was called with the companyId from user's JWT token
        verify(companyService).getCompany(eq(companyId), eq(authentication));
        verify(predefinedItemsService).getItemById(eq(itemId), eq(company));
    }
    
    @Test
    void testAssociateStores_ExtractsCompanyIdFromJwt() {
        // Setup request
        AssociateLocationsRequest request = new AssociateLocationsRequest();
        request.setLocationIds(Arrays.asList(store.getId()));
        
        // Setup authentication to return user
        when(authentication.getPrincipal()).thenReturn(user);
        when(companyService.getCompany(companyId, authentication)).thenReturn(company);
        when(predefinedItemsService.getItemById(itemId, company)).thenReturn(predefinedItem);
        doNothing().when(predefinedItemsService).associateStores(eq(predefinedItem), any(), eq(user));
        
        // Execute - no companyId parameter needed
        ResponseEntity<GenericApiResponse<Void>> response = 
            controller.associateStores(itemId, request, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        
        // Verify that companyService was called with the companyId from user's JWT token
        verify(companyService).getCompany(eq(companyId), eq(authentication));
        verify(predefinedItemsService).getItemById(eq(itemId), eq(company));
        verify(predefinedItemsService).associateStores(eq(predefinedItem), eq(request.getLocationIds()), eq(user));
    }
    
    @Test
    void testGetAssociatedWarehouses_ExtractsCompanyIdFromJwt() {
        // Setup authentication to return user
        when(authentication.getPrincipal()).thenReturn(user);
        when(companyService.getCompany(companyId, authentication)).thenReturn(company);
        when(predefinedItemsService.getItemById(itemId, company)).thenReturn(predefinedItem);
        when(predefinedItemsService.getAssociatedWarehouses(predefinedItem)).thenReturn(Arrays.asList(warehouse));
        
        // Execute - no companyId parameter needed
        ResponseEntity<GenericApiResponse<List<WarehouseResponse>>> response = 
            controller.getAssociatedWarehouses(itemId, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        
        // Verify that companyService was called with the companyId from user's JWT token
        verify(companyService).getCompany(eq(companyId), eq(authentication));
        verify(predefinedItemsService).getItemById(eq(itemId), eq(company));
    }
    
    @Test
    void testAssociateWarehouses_ExtractsCompanyIdFromJwt() {
        // Setup request
        AssociateLocationsRequest request = new AssociateLocationsRequest();
        request.setLocationIds(Arrays.asList(warehouse.getId()));
        
        // Setup authentication to return user
        when(authentication.getPrincipal()).thenReturn(user);
        when(companyService.getCompany(companyId, authentication)).thenReturn(company);
        when(predefinedItemsService.getItemById(itemId, company)).thenReturn(predefinedItem);
        doNothing().when(predefinedItemsService).associateWarehouses(eq(predefinedItem), any(), eq(user));
        
        // Execute - no companyId parameter needed
        ResponseEntity<GenericApiResponse<Void>> response = 
            controller.associateWarehouses(itemId, request, authentication);
        
        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        
        // Verify that companyService was called with the companyId from user's JWT token
        verify(companyService).getCompany(eq(companyId), eq(authentication));
        verify(predefinedItemsService).getItemById(eq(itemId), eq(company));
        verify(predefinedItemsService).associateWarehouses(eq(predefinedItem), eq(request.getLocationIds()), eq(user));
    }
    
    @Test
    void testGetAssociatedStores_UnauthenticatedRequest_ReturnsUnauthorized() {
        // Setup authentication to return null
        when(authentication.getPrincipal()).thenReturn(null);
        
        // Execute
        ResponseEntity<GenericApiResponse<List<StoreResponse>>> response = 
            controller.getAssociatedStores(itemId, authentication);
        
        // Verify
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("Authentication required", response.getBody().getMessage());
        
        // Verify that no service calls were made
        verify(companyService, never()).getCompany(any(), any());
        verify(predefinedItemsService, never()).getItemById(any(), any());
    }
}
