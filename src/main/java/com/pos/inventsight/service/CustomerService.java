package com.pos.inventsight.service;

import com.pos.inventsight.dto.CustomerRequest;
import com.pos.inventsight.dto.CustomerResponse;
import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Customer.CustomerType;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CustomerRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get the user's primary company.
     * 
     * Note: This implementation assumes users work with one company at a time.
     * If a user belongs to multiple companies, the first active company membership is used.
     * This is consistent with the current application's tenant model.
     * 
     * @param user the user whose company to retrieve
     * @return the user's primary company
     * @throws IllegalStateException if user is not associated with any company
     */
    private Company getUserCompany(User user) {
        List<CompanyStoreUser> memberships = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
        if (memberships.isEmpty()) {
            throw new IllegalStateException("User is not associated with any company");
        }
        // Return the first company the user belongs to
        return memberships.get(0).getCompany();
    }
    
    /**
     * Create a new customer from DTO
     */
    public CustomerResponse createCustomer(CustomerRequest request, Authentication auth) {
        // Get user and company
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        
        // Validate email uniqueness if provided
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (customerRepository.existsByCompanyAndEmailAndIsActiveTrue(company, request.getEmail())) {
                throw new DuplicateResourceException(
                    "Customer with email '" + request.getEmail() + "' already exists");
            }
        }
        
        // Validate phone uniqueness if provided
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(company, request.getPhone())) {
                throw new DuplicateResourceException(
                    "Customer with phone '" + request.getPhone() + "' already exists");
            }
        }
        
        // Validate and get store if provided
        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + request.getStoreId()));
            
            // Verify store belongs to same company
            if (!store.getCompany().getId().equals(company.getId())) {
                throw new IllegalArgumentException("Store does not belong to your company");
            }
        }
        
        // Create customer
        Customer customer = new Customer(request.getName(), company, user, CustomerType.REGISTERED);
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPostalCode(request.getPostalCode());
        customer.setCountry(request.getCountry());
        customer.setNotes(request.getNotes());
        customer.setStore(store);
        
        Customer saved = customerRepository.save(customer);
        logger.info("Created customer '{}' in company {}", request.getName(), company.getId());
        
        return new CustomerResponse(saved);
    }
    
    /**
     * Get all customers for company (paginated)
     */
    public Page<CustomerResponse> getCustomers(Pageable pageable, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        Page<Customer> customers = customerRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(company, pageable);
        return customers.map(CustomerResponse::new);
    }
    
    /**
     * Validate that a store exists and belongs to the given company
     */
    private Store validateStoreOwnership(UUID storeId, Company company) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));
        
        // Verify store belongs to same company
        if (!store.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("Store does not belong to your company");
        }
        
        return store;
    }
    
    /**
     * Search customers with optional store filter
     */
    public Page<CustomerResponse> searchCustomers(String searchTerm, UUID storeId, Pageable pageable, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        
        Page<Customer> customers;
        if (storeId != null) {
            Store store = validateStoreOwnership(storeId, company);
            customers = customerRepository.searchCustomersByStore(company, store, searchTerm, pageable);
        } else {
            customers = customerRepository.searchCustomers(company, searchTerm, pageable);
        }
        return customers.map(CustomerResponse::new);
    }
    
    /**
     * Search customers by name, email, or phone (convenience method without store filter).
     * Delegates to the three-parameter version with storeId=null to search across all stores.
     */
    public Page<CustomerResponse> searchCustomers(String searchTerm, Pageable pageable, Authentication auth) {
        // Pass null for storeId to search across all stores in the company
        return searchCustomers(searchTerm, null, pageable, auth);
    }
    
    /**
     * Get customers filtered by store
     */
    public Page<CustomerResponse> getCustomersByStore(UUID storeId, Pageable pageable, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        
        Store store = validateStoreOwnership(storeId, company);
        Page<Customer> customers = customerRepository.findByCompanyAndStoreAndIsActiveTrueOrderByNameAsc(company, store, pageable);
        return customers.map(CustomerResponse::new);
    }
    
    /**
     * Get single customer by ID
     */
    public CustomerResponse getCustomerById(UUID id, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        Customer customer = customerRepository.findByIdAndCompanyAndIsActiveTrue(id, company)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        return new CustomerResponse(customer);
    }
    
    /**
     * Update customer
     */
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        Customer customer = customerRepository.findByIdAndCompanyAndIsActiveTrue(id, company)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        
        // Check for duplicate email if changed
        if (request.getEmail() != null && !request.getEmail().isEmpty() && 
            !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByCompanyAndEmailAndIsActiveTrue(company, request.getEmail())) {
                throw new DuplicateResourceException(
                    "Customer with email '" + request.getEmail() + "' already exists");
            }
        }
        
        // Check for duplicate phone if changed
        if (request.getPhone() != null && !request.getPhone().isEmpty() && 
            !request.getPhone().equals(customer.getPhoneNumber())) {
            if (customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(company, request.getPhone())) {
                throw new DuplicateResourceException(
                    "Customer with phone '" + request.getPhone() + "' already exists");
            }
        }
        
        // Validate and get store if provided
        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + request.getStoreId()));
            
            // Verify store belongs to same company
            if (!store.getCompany().getId().equals(company.getId())) {
                throw new IllegalArgumentException("Store does not belong to your company");
            }
        }
        
        // Update customer fields
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPostalCode(request.getPostalCode());
        customer.setCountry(request.getCountry());
        customer.setNotes(request.getNotes());
        customer.setStore(store);
        
        Customer updated = customerRepository.save(customer);
        logger.info("Updated customer {} in company {}", id, company.getId());
        
        return new CustomerResponse(updated);
    }
    
    /**
     * Soft delete customer
     */
    public void deleteCustomer(UUID id, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        Company company = getUserCompany(user);
        
        Customer customer = customerRepository.findByIdAndCompanyAndIsActiveTrue(id, company)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        
        customer.softDelete(user);
        customerRepository.save(customer);
        
        logger.info("Soft deleted customer {} in company {} by user {}", 
                   id, company.getId(), user.getUsername());
    }
}
