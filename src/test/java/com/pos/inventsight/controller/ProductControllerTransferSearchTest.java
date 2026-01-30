package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for Product Transfer Search API endpoint
 * Tests the GET /api/products/search-for-transfer endpoint
 */
public class ProductControllerTransferSearchTest {

    private ProductController controller;
    private ProductRepository productRepository;
    private UserService userService;
    private CompanyStoreUserRepository companyStoreUserRepository;
    private UserStoreRoleRepository userStoreRoleRepository;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new ProductController();
        productRepository = mock(ProductRepository.class);
        userService = mock(UserService.class);
        companyStoreUserRepository = mock(CompanyStoreUserRepository.class);
        userStoreRoleRepository = mock(UserStoreRoleRepository.class);
        authentication = mock(Authentication.class);
        
        // Inject mocks using reflection
        ReflectionTestUtils.setField(controller, "productRepository", productRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "companyStoreUserRepository", companyStoreUserRepository);
        ReflectionTestUtils.setField(controller, "userStoreRoleRepository", userStoreRoleRepository);
    }
    
    private Product createTestProduct(UUID id, String name, String sku, int quantity) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName(name);
        product.setSku(sku);
        product.setQuantity(quantity);
        product.setRetailPrice(new BigDecimal("100.00"));
        product.setIsActive(true);
        
        // Create and set store
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", UUID.randomUUID());
        store.setStoreName("Test Store");
        product.setStore(store);
        
        // Create and set company
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        company.setName("Test Company");
        product.setCompany(company);
        
        return product;
    }
    
    private void setupMockUser(UUID companyId) {
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockUser.setUsername("testuser");
        
        Company mockCompany = new Company();
        ReflectionTestUtils.setField(mockCompany, "id", companyId);
        mockCompany.setName("Test Company");
        
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setUser(mockUser);
        membership.setCompany(mockCompany);
        
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
            .thenReturn(Collections.singletonList(membership));
    }

    @Test
    void testSearchForTransfer_WithStoreId_ReturnsProducts() {
        // Given: User searching products in a store
        UUID storeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String query = "orange";
        
        setupMockUser(companyId);
        
        // Create test products
        List<Product> products = new ArrayList<>();
        products.add(createTestProduct(UUID.randomUUID(), "Orange Juice", "ORG-001", 100));
        products.add(createTestProduct(UUID.randomUUID(), "Orange Soda", "ORG-002", 50));
        
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), products.size());
        
        when(productRepository.searchProductsForTransferFromStore(
            eq(storeId), eq(companyId), eq(query), any(Pageable.class)))
            .thenReturn(productPage);
        
        when(productRepository.getReservedQuantityFromStore(any(UUID.class), eq(storeId)))
            .thenReturn(5);
        when(productRepository.getInTransitQuantityFromStore(any(UUID.class), eq(storeId)))
            .thenReturn(0);
        
        // When: Calling search-for-transfer endpoint
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            query, storeId.toString(), null, null, 0, 20, "quantity,desc", authentication);
        
        // Then: Should return successful response with products
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> returnedProducts = (List<Map<String, Object>>) responseBody.get("products");
        assertEquals(2, returnedProducts.size());
        
        // Verify first product has correct availability calculation
        Map<String, Object> firstProduct = returnedProducts.get(0);
        assertEquals("Orange Juice", firstProduct.get("name"));
        assertEquals(100, firstProduct.get("quantity"));
        assertEquals(5, firstProduct.get("reserved"));
        assertEquals(0, firstProduct.get("inTransit"));
        assertEquals(95, firstProduct.get("availableForTransfer")); // 100 - 5 - 0
        
        // Verify pagination
        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) responseBody.get("pagination");
        assertEquals(0, pagination.get("currentPage"));
        assertEquals(2L, pagination.get("totalElements"));
        
        // Verify filters
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) responseBody.get("filters");
        assertEquals(query, filters.get("query"));
        assertEquals("STORE", filters.get("fromLocationType"));
    }

    @Test
    void testSearchForTransfer_WithWarehouseId_ReturnsProducts() {
        // Given: User searching products in a warehouse
        UUID warehouseId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String query = "apple";
        
        setupMockUser(companyId);
        
        // Create test product with warehouse
        Product product = createTestProduct(UUID.randomUUID(), "Apple", "APL-001", 200);
        Warehouse warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", warehouseId);
        warehouse.setName("Test Warehouse");
        product.setWarehouse(warehouse);
        product.setStore(null); // Product is in warehouse, not store
        
        // Create WarehouseInventory mock
        // Sales reserved: 10, Transfer reserved: 8, Total reserved: 18
        WarehouseInventory warehouseInventory = new WarehouseInventory();
        warehouseInventory.setWarehouse(warehouse);
        warehouseInventory.setProduct(product);
        warehouseInventory.setCurrentQuantity(200);
        warehouseInventory.setReservedQuantity(10); // Sales order reservations
        
        List<Product> products = Collections.singletonList(product);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), products.size());
        
        when(productRepository.searchProductsForTransferFromWarehouse(
            eq(warehouseId), eq(companyId), eq(query), any(Pageable.class)))
            .thenReturn(productPage);
        
        when(productRepository.findWarehouseInventory(product.getId(), warehouseId))
            .thenReturn(Optional.of(warehouseInventory));
        
        when(productRepository.getReservedQuantityFromWarehouse(any(UUID.class), eq(warehouseId)))
            .thenReturn(8); // Transfer request reservations
        
        when(productRepository.getInTransitQuantityFromWarehouse(any(UUID.class), eq(warehouseId)))
            .thenReturn(5);
        
        // When
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            query, null, warehouseId.toString(), null, 0, 20, "quantity,desc", authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> returnedProducts = (List<Map<String, Object>>) responseBody.get("products");
        assertEquals(1, returnedProducts.size());
        
        Map<String, Object> firstProduct = returnedProducts.get(0);
        assertEquals(200, firstProduct.get("quantity"));
        assertEquals(18, firstProduct.get("reserved")); // 10 (sales) + 8 (transfers)
        assertEquals(5, firstProduct.get("inTransit"));
        assertEquals(177, firstProduct.get("availableForTransfer")); // 200 - 18 - 5
        
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) responseBody.get("filters");
        assertEquals("WAREHOUSE", filters.get("fromLocationType"));
    }

    @Test
    void testSearchForTransfer_WithoutLocation_ReturnsBadRequest() {
        // Given: Request without storeId or warehouseId
        setupMockUser(UUID.randomUUID());
        
        // When
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            "orange", null, null, null, 0, 20, "quantity,desc", authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("Must specify either"));
    }

    @Test
    void testSearchForTransfer_WithBothLocations_ReturnsBadRequest() {
        // Given: Request with both storeId and warehouseId
        setupMockUser(UUID.randomUUID());
        
        // When
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            "orange", UUID.randomUUID().toString(), UUID.randomUUID().toString(), 
            null, 0, 20, "quantity,desc", authentication);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("Cannot specify both"));
    }

    @Test
    void testSearchForTransfer_MultiTenantIsolation_DeniesAccessToOtherCompany() {
        // Given: User trying to access products from a different company
        UUID userCompanyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        
        setupMockUser(userCompanyId);
        
        // When: User tries to search with a different company ID
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            "orange", UUID.randomUUID().toString(), null, otherCompanyId.toString(), 
            0, 20, "quantity,desc", authentication);
        
        // Then: Should deny access
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("No permission"));
    }

    @Test
    void testSearchForTransfer_CalculatesAvailabilityCorrectly() {
        // Given: Product with quantity, reserved, and in-transit
        UUID storeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        
        setupMockUser(companyId);
        
        Product product = createTestProduct(UUID.randomUUID(), "Test Product", "TEST-001", 100);
        Page<Product> productPage = new PageImpl<>(
            Collections.singletonList(product), PageRequest.of(0, 20), 1);
        
        when(productRepository.searchProductsForTransferFromStore(
            any(UUID.class), any(UUID.class), anyString(), any(Pageable.class)))
            .thenReturn(productPage);
        
        // Reserved: 15, In-Transit: 10, Total: 100
        when(productRepository.getReservedQuantityFromStore(any(UUID.class), any(UUID.class)))
            .thenReturn(15);
        when(productRepository.getInTransitQuantityFromStore(any(UUID.class), any(UUID.class)))
            .thenReturn(10);
        
        // When
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            "test", storeId.toString(), null, null, 0, 20, "quantity,desc", authentication);
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) responseBody.get("products");
        
        Map<String, Object> firstProduct = products.get(0);
        assertEquals(100, firstProduct.get("quantity"));
        assertEquals(15, firstProduct.get("reserved"));
        assertEquals(10, firstProduct.get("inTransit"));
        assertEquals(75, firstProduct.get("availableForTransfer")); // 100 - 15 - 10 = 75
    }

    @Test
    void testSearchForTransfer_NoNegativeAvailability() {
        // Given: Product with quantity less than reserved + in-transit
        UUID storeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        
        setupMockUser(companyId);
        
        Product product = createTestProduct(UUID.randomUUID(), "Test Product", "TEST-001", 20);
        Page<Product> productPage = new PageImpl<>(
            Collections.singletonList(product), PageRequest.of(0, 20), 1);
        
        when(productRepository.searchProductsForTransferFromStore(
            any(UUID.class), any(UUID.class), anyString(), any(Pageable.class)))
            .thenReturn(productPage);
        
        // Reserved: 15, In-Transit: 10 = 25 total (more than quantity of 20)
        when(productRepository.getReservedQuantityFromStore(any(UUID.class), any(UUID.class)))
            .thenReturn(15);
        when(productRepository.getInTransitQuantityFromStore(any(UUID.class), any(UUID.class)))
            .thenReturn(10);
        
        // When
        ResponseEntity<?> response = controller.searchProductsForTransfer(
            "test", storeId.toString(), null, null, 0, 20, "quantity,desc", authentication);
        
        // Then: availableForTransfer should be 0, not negative
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) responseBody.get("products");
        
        Map<String, Object> firstProduct = products.get(0);
        assertEquals(0, firstProduct.get("availableForTransfer")); // Math.max(0, 20 - 15 - 10)
    }
}
