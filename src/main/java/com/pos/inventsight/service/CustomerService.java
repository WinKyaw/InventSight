package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Customer.CustomerType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    @Autowired
    private CustomerRepository customerRepository;
    
    /**
     * Get all active customers for a company (paginated)
     */
    public Page<Customer> getCustomers(Company company, Pageable pageable) {
        return customerRepository.findByCompanyAndIsActiveTrueOrderByNameAsc(company, pageable);
    }
    
    /**
     * Get customers by type
     */
    public Page<Customer> getCustomersByType(Company company, CustomerType type, Pageable pageable) {
        return customerRepository.findByCompanyAndCustomerTypeAndIsActiveTrueOrderByNameAsc(
            company, type, pageable);
    }
    
    /**
     * Search customers by name, phone, or email
     */
    public Page<Customer> searchCustomers(Company company, String searchTerm, Pageable pageable) {
        return customerRepository.searchCustomers(company, searchTerm, pageable);
    }
    
    /**
     * Get a single customer by ID
     */
    public Customer getCustomerById(UUID customerId, Company company) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        
        // Verify customer belongs to company
        if (!customer.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("Customer not found in your company");
        }
        
        return customer;
    }
    
    /**
     * Create a new customer
     */
    public Customer createCustomer(
            String name,
            String phoneNumber,
            String email,
            CustomerType customerType,
            String notes,
            BigDecimal discountPercentage,
            Company company,
            User createdBy) {
        
        // Check for duplicate phone number if provided
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(company, phoneNumber)) {
                throw new DuplicateResourceException(
                    "Customer with phone number '" + phoneNumber + "' already exists");
            }
        }
        
        // Check for duplicate email if provided
        if (email != null && !email.isEmpty()) {
            if (customerRepository.existsByCompanyAndEmailAndIsActiveTrue(company, email)) {
                throw new DuplicateResourceException(
                    "Customer with email '" + email + "' already exists");
            }
        }
        
        Customer customer = new Customer(name, company, createdBy, customerType);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
        customer.setNotes(notes);
        customer.setDiscountPercentage(discountPercentage);
        
        Customer saved = customerRepository.save(customer);
        logger.info("Created customer '{}' ({}) in company {}", name, customerType, company.getId());
        
        return saved;
    }
    
    /**
     * Create a guest customer with minimal information
     */
    public Customer createGuestCustomer(Company company, User createdBy) {
        // Generate a unique guest name
        long guestCount = customerRepository.countByCompanyAndIsActiveTrue(company);
        String guestName = "Guest Customer #" + (guestCount + 1);
        
        Customer customer = new Customer(guestName, company, createdBy, CustomerType.GUEST);
        
        Customer saved = customerRepository.save(customer);
        logger.info("Created guest customer '{}' in company {}", guestName, company.getId());
        
        return saved;
    }
    
    /**
     * Update a customer
     */
    public Customer updateCustomer(
            UUID customerId,
            String name,
            String phoneNumber,
            String email,
            CustomerType customerType,
            String notes,
            BigDecimal discountPercentage,
            Company company) {
        
        Customer customer = getCustomerById(customerId, company);
        
        // Check for duplicate phone number if changed
        if (phoneNumber != null && !phoneNumber.isEmpty() && 
            !phoneNumber.equals(customer.getPhoneNumber())) {
            if (customerRepository.existsByCompanyAndPhoneNumberAndIsActiveTrue(company, phoneNumber)) {
                throw new DuplicateResourceException(
                    "Customer with phone number '" + phoneNumber + "' already exists");
            }
        }
        
        // Check for duplicate email if changed
        if (email != null && !email.isEmpty() && 
            !email.equals(customer.getEmail())) {
            if (customerRepository.existsByCompanyAndEmailAndIsActiveTrue(company, email)) {
                throw new DuplicateResourceException(
                    "Customer with email '" + email + "' already exists");
            }
        }
        
        customer.setName(name);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
        customer.setCustomerType(customerType);
        customer.setNotes(notes);
        customer.setDiscountPercentage(discountPercentage);
        
        Customer updated = customerRepository.save(customer);
        logger.info("Updated customer {} in company {}", customerId, company.getId());
        
        return updated;
    }
    
    /**
     * Soft delete a customer
     */
    public void deleteCustomer(UUID customerId, Company company, User deletedBy) {
        Customer customer = getCustomerById(customerId, company);
        
        customer.softDelete(deletedBy);
        customerRepository.save(customer);
        
        logger.info("Soft deleted customer {} in company {} by user {}", 
                   customerId, company.getId(), deletedBy.getUsername());
    }
    
    /**
     * Add a purchase to customer's total
     */
    public Customer addPurchase(UUID customerId, BigDecimal amount, Company company) {
        Customer customer = getCustomerById(customerId, company);
        
        customer.addPurchase(amount);
        Customer updated = customerRepository.save(customer);
        
        logger.info("Added purchase of {} to customer {} in company {}", 
                   amount, customerId, company.getId());
        
        return updated;
    }
}
