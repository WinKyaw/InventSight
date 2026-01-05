package com.pos.inventsight.service;

import com.pos.inventsight.dto.StoreInventoryAdditionRequest;
import com.pos.inventsight.dto.StoreInventoryAdditionResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.StoreInventoryAdditionRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing store inventory additions (restocks)
 */
@Service
public class StoreInventoryService {

    private static final Logger logger = LoggerFactory.getLogger(StoreInventoryService.class);

    @Autowired
    private StoreInventoryAdditionRepository additionRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;

    /**
     * Add inventory to store (restock)
     */
    @Transactional
    public StoreInventoryAdditionResponse addInventory(StoreInventoryAdditionRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();
        
        // Check if user is EMPLOYEE and trying to set cost
        User user = (User) authentication.getPrincipal();
        CompanyRole userRole = getUserCompanyRole(user);
        if (userRole == CompanyRole.EMPLOYEE && request.getUnitCost() != null) {
            // Employees cannot set cost fields - ignore silently
            request.setUnitCost(null);
            logger.warn("EMPLOYEE {} attempted to set unitCost, field ignored", username);
        }

        // Get store and product
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + request.getStoreId()));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Verify product belongs to store
        if (product.getStore() == null || !product.getStore().getId().equals(store.getId())) {
            throw new IllegalArgumentException("Product does not belong to the specified store");
        }

        // Create addition record
        StoreInventoryAddition addition = new StoreInventoryAddition(store, product, request.getQuantity());
        addition.setUnitCost(request.getUnitCost());
        addition.setSupplierName(request.getSupplierName());
        addition.setReferenceNumber(request.getReferenceNumber());
        addition.setReceiptDate(request.getReceiptDate() != null ? request.getReceiptDate() : LocalDate.now());
        addition.setExpiryDate(request.getExpiryDate());
        addition.setBatchNumber(request.getBatchNumber());
        addition.setNotes(request.getNotes());
        addition.setTransactionType(request.getTransactionType() != null ? 
            request.getTransactionType() : StoreInventoryAddition.TransactionType.RESTOCK);
        addition.setCreatedBy(username);

        addition = additionRepository.save(addition);

        // Update product quantity
        Integer currentQuantity = product.getQuantity() != null ? product.getQuantity() : 0;
        product.setQuantity(currentQuantity + request.getQuantity());
        productRepository.save(product);

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "inventory_added",
            "store_inventory",
            String.format("Added %d units of %s to store %s", 
                request.getQuantity(), product.getName(), store.getStoreName())
        );

        logger.info("Successfully added {} units of product {} to store {}", 
            request.getQuantity(), product.getName(), store.getStoreName());

        return new StoreInventoryAdditionResponse(addition);
    }

    /**
     * Get addition history for a store with pagination and filtering
     */
    public Page<StoreInventoryAddition> getAdditions(
            UUID storeId,
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String filterByUsername,
            Pageable pageable) {
        
        // If filtering by user
        if (filterByUsername != null && !filterByUsername.isEmpty()) {
            return additionRepository.findByStoreIdAndCreatedBy(storeId, filterByUsername, pageable);
        }
        
        // TODO: Add support for date range and transaction type filtering
        // For now, return all additions for the store
        // Future enhancement: Create custom query methods or use Specifications for complex filtering
        return additionRepository.findByStoreId(storeId, pageable);
    }

    /**
     * Get user's company role - returns highest role in company hierarchy
     */
    public CompanyRole getUserCompanyRole(User user) {
        // Get user's active company memberships
        List<CompanyStoreUser> memberships = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
        if (memberships.isEmpty()) {
            return CompanyRole.EMPLOYEE; // Default to lowest privilege
        }
        
        // Return the highest role based on hierarchy
        CompanyRole highestRole = CompanyRole.EMPLOYEE;
        for (CompanyStoreUser membership : memberships) {
            CompanyRole role = membership.getRole();
            
            // Check in order of hierarchy (highest to lowest)
            if (role == CompanyRole.FOUNDER) {
                return CompanyRole.FOUNDER; // Highest possible, return immediately
            }
            if (role == CompanyRole.CEO && highestRole != CompanyRole.FOUNDER) {
                highestRole = CompanyRole.CEO;
            }
            if (role == CompanyRole.GENERAL_MANAGER && 
                highestRole != CompanyRole.FOUNDER &&
                highestRole != CompanyRole.CEO) {
                highestRole = CompanyRole.GENERAL_MANAGER;
            }
            if (role == CompanyRole.STORE_MANAGER &&
                highestRole == CompanyRole.EMPLOYEE) {
                highestRole = CompanyRole.STORE_MANAGER;
            }
        }
        
        return highestRole;
    }
}
