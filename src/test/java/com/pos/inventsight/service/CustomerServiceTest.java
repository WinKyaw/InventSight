package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Customer.CustomerType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CustomerRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Customer Service Unit Tests")
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
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
    }
    
    @Test
    @DisplayName("Should get all customers for a company")
    void shouldGetCustomersForCompany() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(testCustomer));
        
        when(customerRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(testCompany, pageable))
            .thenReturn(customerPage);
        
        Page<Customer> result = customerService.getCustomers(testCompany, pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCustomer.getName(), result.getContent().get(0).getName());
        
        verify(customerRepository).findByCompanyAndIsActiveTrueOrderByNameAsc(testCompany, pageable);
    }
    
    @Test
    @DisplayName("Should create a customer")
    void shouldCreateCustomer() {
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(false);
        when(customerRepository.existsByCompanyAndEmailAndIsActiveTrue(testCompany, "john@example.com"))
            .thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        Customer result = customerService.createCustomer(
            "John Doe", "555-1234", "john@example.com", 
            CustomerType.REGISTERED, "Test notes", BigDecimal.TEN, 
            testCompany, testUser);
        
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should throw exception when creating customer with duplicate phone")
    void shouldThrowExceptionWhenCreatingCustomerWithDuplicatePhone() {
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(true);
        
        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(
                "John Doe", "555-1234", "john@example.com", 
                CustomerType.REGISTERED, null, null, testCompany, testUser);
        });
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should throw exception when creating customer with duplicate email")
    void shouldThrowExceptionWhenCreatingCustomerWithDuplicateEmail() {
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-1234"))
            .thenReturn(false);
        when(customerRepository.existsByCompanyAndEmailAndIsActiveTrue(testCompany, "john@example.com"))
            .thenReturn(true);
        
        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(
                "John Doe", "555-1234", "john@example.com", 
                CustomerType.REGISTERED, null, null, testCompany, testUser);
        });
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should create guest customer")
    void shouldCreateGuestCustomer() {
        when(customerRepository.countByCompanyAndIsActiveTrue(testCompany)).thenReturn(5L);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        Customer result = customerService.createGuestCustomer(testCompany, testUser);
        
        assertNotNull(result);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should get customer by ID")
    void shouldGetCustomerById() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        
        Customer result = customerService.getCustomerById(customerId, testCompany);
        
        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerById(customerId, testCompany);
        });
    }
    
    @Test
    @DisplayName("Should update customer")
    void shouldUpdateCustomer() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(testCompany, "555-5678"))
            .thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        Customer result = customerService.updateCustomer(
            customerId, "Jane Doe", "555-5678", "jane@example.com",
            CustomerType.REGISTERED, "Updated notes", BigDecimal.ZERO, testCompany);
        
        assertNotNull(result);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should soft delete customer")
    void shouldSoftDeleteCustomer() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        customerService.deleteCustomer(customerId, testCompany, testUser);
        
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should add purchase to customer")
    void shouldAddPurchaseToCustomer() {
        UUID customerId = testCustomer.getId();
        BigDecimal purchaseAmount = new BigDecimal("100.00");
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        Customer result = customerService.addPurchase(customerId, purchaseAmount, testCompany);
        
        assertNotNull(result);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Should search customers")
    void shouldSearchCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(testCustomer));
        
        when(customerRepository.searchCustomers(testCompany, "John", pageable))
            .thenReturn(customerPage);
        
        Page<Customer> result = customerService.searchCustomers(testCompany, "John", pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(customerRepository).searchCustomers(testCompany, "John", pageable);
    }
}
