package com.pos.inventsight.service;

import com.pos.inventsight.dto.CustomerRequest;
import com.pos.inventsight.dto.CustomerResponse;
import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Customer.CustomerType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CustomerRepository;
import com.pos.inventsight.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Customer Service Unit Tests")
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private CustomerService customerService;
    
    private User testUser;
    private Company testCompany;
    private Customer testCustomer;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        
        // Setup test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");
        
        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setName("John Doe");
        testCustomer.setPhoneNumber("555-1234");
        testCustomer.setEmail("john@example.com");
        testCustomer.setCustomerType(CustomerType.REGISTERED);
        testCustomer.setCompany(testCompany);
        testCustomer.setCreatedByUser(testUser);
        
        // Mock authentication
        when(authentication.getName()).thenReturn("testuser");
        
        // Mock UserService
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        
        // Mock CompanyStoreUserRepository to return user's company
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setCompany(testCompany);
        membership.setUser(testUser);
        membership.setIsActive(true);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(List.of(membership));
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    @DisplayName("Should get all customers for a company")
    void shouldGetCustomersForCompany() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(testCustomer));
        
        when(customerRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(testCompany, pageable))
            .thenReturn(customerPage);
        
        Page<CustomerResponse> result = customerService.getCustomers(pageable, authentication);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCustomer.getName(), result.getContent().get(0).getName());
        
        verify(customerRepository).findByCompanyAndIsActiveTrueOrderByNameAsc(testCompany, pageable);
    }
    
    @Test
    @DisplayName("Should create a customer")
    void shouldCreateCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Doe");
        request.setPhone("555-1234");
        request.setEmail("john@example.com");
        request.setNotes("Test notes");
        
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(false);
        when(customerRepository.existsByCompanyAndEmailAndIsActiveTrue(testCompany, "john@example.com"))
            .thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        CustomerResponse result = customerService.createCustomer(request, authentication);
        
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should throw exception when creating customer with duplicate phone")
    void shouldThrowExceptionWhenCreatingCustomerWithDuplicatePhone() {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Doe");
        request.setPhone("555-1234");
        request.setEmail("john@example.com");
        
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(true);
        
        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(request, authentication);
        });
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should throw exception when creating customer with duplicate email")
    void shouldThrowExceptionWhenCreatingCustomerWithDuplicateEmail() {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Doe");
        request.setPhone("555-1234");
        request.setEmail("john@example.com");
        
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(false);
        when(customerRepository.existsByCompanyAndEmailAndIsActiveTrue(testCompany, "john@example.com"))
            .thenReturn(true);
        
        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(request, authentication);
        });
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should create customer without email")
    void shouldCreateCustomerWithoutEmail() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Guest Customer");
        request.setPhone("555-0000");
        // email is null
        
        Customer guestCustomer = new Customer();
        guestCustomer.setId(UUID.randomUUID());
        guestCustomer.setName("Guest Customer");
        guestCustomer.setPhoneNumber("555-0000");
        guestCustomer.setCompany(testCompany);
        guestCustomer.setCreatedByUser(testUser);
        
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-0000"))
            .thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(guestCustomer);
        
        CustomerResponse result = customerService.createCustomer(request, authentication);
        
        assertNotNull(result);
        assertEquals("Guest Customer", result.getName());
        assertNull(result.getEmail());
        
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should get customer by ID")
    void shouldGetCustomerById() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findByIdAndCompanyAndIsActiveTrue(customerId, testCompany))
            .thenReturn(Optional.of(testCustomer));
        
        CustomerResponse result = customerService.getCustomerById(customerId, authentication);
        
        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        
        verify(customerRepository).findByIdAndCompanyAndIsActiveTrue(customerId, testCompany);
    }
    
    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdAndCompanyAndIsActiveTrue(customerId, testCompany))
            .thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerById(customerId, authentication);
        });
    }
    
    @Test
    @DisplayName("Should update customer")
    void shouldUpdateCustomer() {
        UUID customerId = testCustomer.getId();
        
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane Doe");
        request.setPhone("555-5678");
        request.setEmail("jane@example.com");
        request.setNotes("Updated notes");
        
        Customer updatedCustomer = new Customer();
        updatedCustomer.setId(customerId);
        updatedCustomer.setName("Jane Doe");
        updatedCustomer.setEmail("jane@example.com");
        updatedCustomer.setPhoneNumber("555-5678");
        updatedCustomer.setNotes("Updated notes");
        updatedCustomer.setCompany(testCompany);
        updatedCustomer.setCreatedByUser(testUser);
        
        when(customerRepository.findByIdAndCompanyAndIsActiveTrue(customerId, testCompany))
            .thenReturn(Optional.of(testCustomer));
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-5678"))
            .thenReturn(false);
        when(customerRepository.existsByCompanyAndEmailAndIsActiveTrue(testCompany, "jane@example.com"))
            .thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);
        
        CustomerResponse result = customerService.updateCustomer(customerId, request, authentication);
        
        assertNotNull(result);
        assertEquals("Jane Doe", result.getName());
        assertEquals("jane@example.com", result.getEmail());
        
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should soft delete customer")
    void shouldSoftDeleteCustomer() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findByIdAndCompanyAndIsActiveTrue(customerId, testCompany))
            .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        customerService.deleteCustomer(customerId, authentication);
        
        verify(customerRepository).save(argThat(customer -> 
            !customer.getIsActive()
        ));
    }
    
    @Test
    @DisplayName("Should search customers")
    void shouldSearchCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(testCustomer));
        
        when(customerRepository.searchCustomers(testCompany, "John", pageable))
            .thenReturn(customerPage);
        
        Page<CustomerResponse> result = customerService.searchCustomers("John", pageable, authentication);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(customerRepository).searchCustomers(testCompany, "John", pageable);
    }
}
