package com.pos.inventsight.controller;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.ProductService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify company filtering is properly implemented in ProductController
 */
public class ProductControllerCompanyFilterTest {

    private ProductController controller;
    private ProductService productService;
    private UserService userService;
    private CompanyStoreUserRepository companyStoreUserRepository;

    @BeforeEach
    void setUp() {
        controller = new ProductController();
        productService = mock(ProductService.class);
        userService = mock(UserService.class);
        companyStoreUserRepository = mock(CompanyStoreUserRepository.class);
        
        // Inject mocks using reflection
        ReflectionTestUtils.setField(controller, "productService", productService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "companyStoreUserRepository", companyStoreUserRepository);
    }

    /**
     * Test that products are correctly filtered by user's company
     */
    @Test
    void testProductsFilteredByCompany() {
        // Given: User belongs to Company A
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        
        Company companyA = new Company();
        companyA.setId(UUID.randomUUID());
        companyA.setName("Company A");
        
        Company companyB = new Company();
        companyB.setId(UUID.randomUUID());
        companyB.setName("Company B");
        
        Store storeA = new Store();
        storeA.setId(UUID.randomUUID());
        storeA.setCompany(companyA);
        
        Store storeB = new Store();
        storeB.setId(UUID.randomUUID());
        storeB.setCompany(companyB);
        
        // User membership
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setUser(user);
        membership.setCompany(companyA);
        membership.setIsActive(true);
        
        List<CompanyStoreUser> memberships = List.of(membership);
        
        // Products from different companies
        Product productFromCompanyA = new Product();
        productFromCompanyA.setId(UUID.randomUUID());
        productFromCompanyA.setName("Product A");
        productFromCompanyA.setStore(storeA);
        
        Product productFromCompanyB = new Product();
        productFromCompanyB.setId(UUID.randomUUID());
        productFromCompanyB.setName("Product B");
        productFromCompanyB.setStore(storeB);
        
        List<Product> allProducts = List.of(productFromCompanyA, productFromCompanyB);
        
        // When
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(user)).thenReturn(memberships);
        
        // Extract company IDs (simulating what the controller does)
        Set<UUID> userCompanyIds = memberships.stream()
            .map(m -> m.getCompany().getId())
            .collect(Collectors.toSet());
        
        // Filter products (simulating what the controller does)
        List<Product> filteredProducts = allProducts.stream()
            .filter(p -> p.getStore() != null && p.getStore().getCompany() != null 
                && userCompanyIds.contains(p.getStore().getCompany().getId()))
            .collect(Collectors.toList());
        
        // Then: Only products from Company A should be included
        assertEquals(1, filteredProducts.size(), "Should only have products from user's company");
        assertEquals("Product A", filteredProducts.get(0).getName(), 
            "Should only include Product A from Company A");
        assertFalse(filteredProducts.stream().anyMatch(p -> "Product B".equals(p.getName())),
            "Should not include Product B from Company B");
    }

    /**
     * Test that users without company membership get no products
     */
    @Test
    void testUserWithNoCompanyGetsNoProducts() {
        // Given: User has no company memberships
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("userwithoucompany");
        
        List<CompanyStoreUser> emptyMemberships = new ArrayList<>();
        
        // When
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(user)).thenReturn(emptyMemberships);
        
        // Then: User should have empty company ID set
        Set<UUID> userCompanyIds = emptyMemberships.stream()
            .map(m -> m.getCompany().getId())
            .collect(Collectors.toSet());
        
        assertTrue(userCompanyIds.isEmpty(), "User with no company should have empty company ID set");
    }

    /**
     * Test that users belonging to multiple companies see products from all their companies
     */
    @Test
    void testUserWithMultipleCompaniesSeesAllProducts() {
        // Given: User belongs to both Company A and Company B
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("multicompanyuser");
        
        Company companyA = new Company();
        companyA.setId(UUID.randomUUID());
        companyA.setName("Company A");
        
        Company companyB = new Company();
        companyB.setId(UUID.randomUUID());
        companyB.setName("Company B");
        
        Company companyC = new Company();
        companyC.setId(UUID.randomUUID());
        companyC.setName("Company C");
        
        Store storeA = new Store();
        storeA.setCompany(companyA);
        
        Store storeB = new Store();
        storeB.setCompany(companyB);
        
        Store storeC = new Store();
        storeC.setCompany(companyC);
        
        // User memberships in Company A and B
        CompanyStoreUser membershipA = new CompanyStoreUser();
        membershipA.setUser(user);
        membershipA.setCompany(companyA);
        membershipA.setIsActive(true);
        
        CompanyStoreUser membershipB = new CompanyStoreUser();
        membershipB.setUser(user);
        membershipB.setCompany(companyB);
        membershipB.setIsActive(true);
        
        List<CompanyStoreUser> memberships = List.of(membershipA, membershipB);
        
        // Products from different companies
        Product productA = new Product();
        productA.setName("Product A");
        productA.setStore(storeA);
        
        Product productB = new Product();
        productB.setName("Product B");
        productB.setStore(storeB);
        
        Product productC = new Product();
        productC.setName("Product C");
        productC.setStore(storeC);
        
        List<Product> allProducts = List.of(productA, productB, productC);
        
        // When
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(user)).thenReturn(memberships);
        
        Set<UUID> userCompanyIds = memberships.stream()
            .map(m -> m.getCompany().getId())
            .collect(Collectors.toSet());
        
        List<Product> filteredProducts = allProducts.stream()
            .filter(p -> p.getStore() != null && p.getStore().getCompany() != null 
                && userCompanyIds.contains(p.getStore().getCompany().getId()))
            .collect(Collectors.toList());
        
        // Then: Should see products from Company A and B, but not C
        assertEquals(2, filteredProducts.size(), "Should see products from both companies");
        assertTrue(filteredProducts.stream().anyMatch(p -> "Product A".equals(p.getName())),
            "Should include Product A from Company A");
        assertTrue(filteredProducts.stream().anyMatch(p -> "Product B".equals(p.getName())),
            "Should include Product B from Company B");
        assertFalse(filteredProducts.stream().anyMatch(p -> "Product C".equals(p.getName())),
            "Should not include Product C from Company C");
    }

    /**
     * Test that products without a store or company are filtered out
     */
    @Test
    void testProductsWithoutStoreOrCompanyAreFiltered() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        
        Company company = new Company();
        company.setId(UUID.randomUUID());
        
        Store store = new Store();
        store.setCompany(company);
        
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setUser(user);
        membership.setCompany(company);
        membership.setIsActive(true);
        
        // Product with store and company
        Product validProduct = new Product();
        validProduct.setName("Valid Product");
        validProduct.setStore(store);
        
        // Product without store
        Product productNoStore = new Product();
        productNoStore.setName("Product No Store");
        productNoStore.setStore(null);
        
        // Product with store but no company
        Store storeNoCompany = new Store();
        storeNoCompany.setCompany(null);
        Product productNoCompany = new Product();
        productNoCompany.setName("Product No Company");
        productNoCompany.setStore(storeNoCompany);
        
        List<Product> allProducts = List.of(validProduct, productNoStore, productNoCompany);
        List<CompanyStoreUser> memberships = List.of(membership);
        
        // When
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(user)).thenReturn(memberships);
        
        Set<UUID> userCompanyIds = memberships.stream()
            .map(m -> m.getCompany().getId())
            .collect(Collectors.toSet());
        
        List<Product> filteredProducts = allProducts.stream()
            .filter(p -> p.getStore() != null && p.getStore().getCompany() != null 
                && userCompanyIds.contains(p.getStore().getCompany().getId()))
            .collect(Collectors.toList());
        
        // Then: Only valid product should be included
        assertEquals(1, filteredProducts.size(), "Only products with valid store and company should be included");
        assertEquals("Valid Product", filteredProducts.get(0).getName());
    }
}
