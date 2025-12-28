package com.pos.inventsight.controller;

import com.pos.inventsight.dto.GenericApiResponse;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Customer.CustomerType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.CompanyService;
import com.pos.inventsight.service.CustomerService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private UserService userService;
    
    /**
     * List customers for a company (paginated, searchable)
     */
    @GetMapping
    @Operation(summary = "List customers", description = "Get all customers for a company")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> listCustomers(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @Parameter(description = "Customer type filter") @RequestParam(required = false) CustomerType type,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Customer> customersPage;
            
            if (search != null && !search.trim().isEmpty()) {
                customersPage = customerService.searchCustomers(company, search, pageable);
            } else if (type != null) {
                customersPage = customerService.getCustomersByType(company, type, pageable);
            } else {
                customersPage = customerService.getCustomers(company, pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("customers", customersPage.getContent());
            response.put("currentPage", customersPage.getNumber());
            response.put("totalItems", customersPage.getTotalElements());
            response.put("totalPages", customersPage.getTotalPages());
            
            return ResponseEntity.ok(GenericApiResponse.success(response, "Customers retrieved successfully"));
            
        } catch (Exception e) {
            logger.error("Error listing customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericApiResponse.error("Failed to retrieve customers: " + e.getMessage()));
        }
    }
    
    /**
     * Get a single customer by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer", description = "Get a single customer by ID")
    public ResponseEntity<GenericApiResponse<Customer>> getCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            Customer customer = customerService.getCustomerById(id, company);
            
            return ResponseEntity.ok(GenericApiResponse.success(customer, "Customer retrieved successfully"));
            
        } catch (Exception e) {
            logger.error("Error getting customer {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericApiResponse.error("Customer not found: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new customer
     */
    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer")
    public ResponseEntity<GenericApiResponse<Customer>> createCustomer(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            User user = userService.getUserByUsername(authentication.getName());
            
            String name = (String) request.get("name");
            String phoneNumber = (String) request.get("phoneNumber");
            String email = (String) request.get("email");
            String typeStr = (String) request.getOrDefault("customerType", "GUEST");
            CustomerType customerType = CustomerType.valueOf(typeStr);
            String notes = (String) request.get("notes");
            
            BigDecimal discountPercentage = null;
            if (request.containsKey("discountPercentage")) {
                Object discount = request.get("discountPercentage");
                discountPercentage = discount != null ? new BigDecimal(discount.toString()) : BigDecimal.ZERO;
            }
            
            Customer customer = customerService.createCustomer(
                name, phoneNumber, email, customerType, notes, discountPercentage, company, user);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericApiResponse.success(customer, "Customer created successfully"));
            
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericApiResponse.error("Failed to create customer: " + e.getMessage()));
        }
    }
    
    /**
     * Quick create guest customer
     */
    @PostMapping("/guest")
    @Operation(summary = "Create guest customer", description = "Quickly create a guest customer with minimal information")
    public ResponseEntity<GenericApiResponse<Customer>> createGuestCustomer(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            User user = userService.getUserByUsername(authentication.getName());
            
            Customer customer = customerService.createGuestCustomer(company, user);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericApiResponse.success(customer, "Guest customer created successfully"));
            
        } catch (Exception e) {
            logger.error("Error creating guest customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericApiResponse.error("Failed to create guest customer: " + e.getMessage()));
        }
    }
    
    /**
     * Update a customer
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update an existing customer")
    public ResponseEntity<GenericApiResponse<Customer>> updateCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            
            String name = (String) request.get("name");
            String phoneNumber = (String) request.get("phoneNumber");
            String email = (String) request.get("email");
            String typeStr = (String) request.getOrDefault("customerType", "GUEST");
            CustomerType customerType = CustomerType.valueOf(typeStr);
            String notes = (String) request.get("notes");
            
            BigDecimal discountPercentage = null;
            if (request.containsKey("discountPercentage")) {
                Object discount = request.get("discountPercentage");
                discountPercentage = discount != null ? new BigDecimal(discount.toString()) : BigDecimal.ZERO;
            }
            
            Customer customer = customerService.updateCustomer(
                id, name, phoneNumber, email, customerType, notes, discountPercentage, company);
            
            return ResponseEntity.ok(GenericApiResponse.success(customer, "Customer updated successfully"));
            
        } catch (Exception e) {
            logger.error("Error updating customer {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericApiResponse.error("Failed to update customer: " + e.getMessage()));
        }
    }
    
    /**
     * Soft delete a customer
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Soft delete a customer")
    public ResponseEntity<GenericApiResponse<Void>> deleteCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            User user = userService.getUserByUsername(authentication.getName());
            
            customerService.deleteCustomer(id, company, user);
            
            return ResponseEntity.ok(GenericApiResponse.success(null, "Customer deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Error deleting customer {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericApiResponse.error("Failed to delete customer: " + e.getMessage()));
        }
    }
}
