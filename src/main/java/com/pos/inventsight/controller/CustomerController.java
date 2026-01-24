package com.pos.inventsight.controller;

import com.pos.inventsight.dto.CustomerRequest;
import com.pos.inventsight.dto.CustomerResponse;
import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
    
    /**
     * Create a new customer
     */
    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer for the company")
    public ResponseEntity<Map<String, Object>> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication) {
        
        try {
            CustomerResponse customer = customerService.createCustomer(request, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customer created successfully");
            response.put("customer", customer);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (DuplicateResourceException e) {
            logger.error("Duplicate customer: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create customer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * List all customers (paginated)
     * Supports optional filtering by storeId and search query
     */
    @GetMapping
    @Operation(summary = "List customers", description = "Get all customers for the company with pagination and optional filtering")
    public ResponseEntity<Map<String, Object>> listCustomers(
            @Parameter(description = "Optional store ID filter") @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Optional search query") @RequestParam(required = false) String search,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication) {
        
        try {
            logger.debug("Listing customers with pagination");
            
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<CustomerResponse> customersPage;
            
            // If search query is provided, use search; otherwise use regular listing
            if (search != null && !search.trim().isEmpty()) {
                logger.debug("Performing customer search");
                customersPage = customerService.searchCustomers(search, storeId, pageable, authentication);
            } else if (storeId != null) {
                logger.debug("Filtering customers by store");
                customersPage = customerService.getCustomersByStore(storeId, pageable, authentication);
            } else {
                logger.debug("Loading all customers");
                customersPage = customerService.getCustomers(pageable, authentication);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customers retrieved successfully");
            response.put("customers", customersPage.getContent());
            response.put("currentPage", customersPage.getNumber());
            response.put("totalItems", customersPage.getTotalElements());
            response.put("totalPages", customersPage.getTotalPages());
            response.put("hasMore", customersPage.hasNext());
            
            logger.info("✅ Returning {} customers", customersPage.getContent().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("State error while listing customers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Unable to retrieve customers. Please check your company settings.");
            errorResponse.put("customers", Collections.emptyList());
            errorResponse.put("totalItems", 0);
            errorResponse.put("totalPages", 0);
            errorResponse.put("currentPage", 0);
            errorResponse.put("hasMore", false);
            return ResponseEntity.ok(errorResponse);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument while listing customers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid request parameters");
            errorResponse.put("customers", Collections.emptyList());
            errorResponse.put("totalItems", 0);
            errorResponse.put("totalPages", 0);
            errorResponse.put("currentPage", 0);
            errorResponse.put("hasMore", false);
            return ResponseEntity.ok(errorResponse);
        } catch (Exception e) {
            logger.error("❌ Error listing customers", e);
            // Return 200 with empty list to prevent frontend errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve customers. Please try again.");
            errorResponse.put("customers", Collections.emptyList());
            errorResponse.put("totalItems", 0);
            errorResponse.put("totalPages", 0);
            errorResponse.put("currentPage", 0);
            errorResponse.put("hasMore", false);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Search customers
     */
    @GetMapping("/search")
    @Operation(summary = "Search customers", description = "Search customers by name, email, or phone")
    public ResponseEntity<Map<String, Object>> searchCustomers(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CustomerResponse> customersPage = customerService.searchCustomers(q, pageable, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customers search completed");
            response.put("query", q);
            response.put("customers", customersPage.getContent());
            response.put("currentPage", customersPage.getNumber());
            response.put("totalItems", customersPage.getTotalElements());
            response.put("totalPages", customersPage.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error searching customers: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to search customers");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get a single customer by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer", description = "Get a single customer by ID")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            Authentication authentication) {
        
        try {
            CustomerResponse customer = customerService.getCustomerById(id, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customer retrieved successfully");
            response.put("customer", customer);
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Customer not found: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error getting customer {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve customer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update a customer
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update an existing customer")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication) {
        
        try {
            CustomerResponse customer = customerService.updateCustomer(id, request, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customer updated successfully");
            response.put("customer", customer);
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Customer not found: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (DuplicateResourceException e) {
            logger.error("Duplicate customer: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error updating customer {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update customer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Soft delete a customer
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Soft delete a customer")
    public ResponseEntity<Map<String, Object>> deleteCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID id,
            Authentication authentication) {
        
        try {
            customerService.deleteCustomer(id, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Customer deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Customer not found: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error deleting customer {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete customer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
