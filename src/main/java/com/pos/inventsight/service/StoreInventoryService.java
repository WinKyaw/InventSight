package com.pos.inventsight.service;

import com.pos.inventsight.dto.StoreInventoryAdditionRequest;
import com.pos.inventsight.dto.StoreInventoryAdditionResponse;
import com.pos.inventsight.dto.StoreInventoryBatchAddRequest;
import com.pos.inventsight.dto.StoreInventoryBatchAddResponse;
import com.pos.inventsight.dto.StoreInventoryWithdrawalRequest;
import com.pos.inventsight.dto.StoreInventoryWithdrawalResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.StoreInventoryAdditionRepository;
import com.pos.inventsight.repository.sql.StoreInventoryWithdrawalRepository;
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
    private StoreInventoryWithdrawalRepository withdrawalRepository;

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
     * Add multiple items to store inventory in a single transaction (batch restock)
     */
    @Transactional
    public StoreInventoryBatchAddResponse addInventoryBatch(
            StoreInventoryBatchAddRequest request, 
            Authentication authentication) {
        
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();
        User user = (User) authentication.getPrincipal();
        CompanyRole userRole = getUserCompanyRole(user);
        
        logger.info("📦 Batch restocking {} items for store: {}", 
            request.getItems().size(), request.getStoreId());
        
        // Verify store exists
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Store not found with ID: " + request.getStoreId()));
        
        StoreInventoryBatchAddResponse response = new StoreInventoryBatchAddResponse();
        response.setTotalItems(request.getItems().size());
        
        int successCount = 0;
        int failCount = 0;
        
        // Process each item
        for (StoreInventoryBatchAddRequest.BatchItem item : request.getItems()) {
            String productName = "Unknown";
            try {
                // Get product
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + item.getProductId()));
                
                // Extract product name early for error reporting
                productName = product.getName();
                
                // Verify product belongs to store
                if (product.getStore() == null || !product.getStore().getId().equals(store.getId())) {
                    throw new IllegalArgumentException(
                        "Product does not belong to the specified store");
                }
                
                // Determine notes (item-specific overrides global)
                String notes = item.getNotes() != null ? 
                    item.getNotes() : request.getGlobalNotes();
                
                // Create addition record
                StoreInventoryAddition addition = new StoreInventoryAddition(
                    store, product, item.getQuantity());
                addition.setNotes(notes);
                addition.setTransactionType(StoreInventoryAddition.TransactionType.RESTOCK);
                addition.setCreatedBy(username);
                
                addition = additionRepository.save(addition);
                
                // Update product quantity
                Integer currentQuantity = product.getQuantity() != null ? 
                    product.getQuantity() : 0;
                product.setQuantity(currentQuantity + item.getQuantity());
                productRepository.save(product);
                
                // Log activity
                activityLogService.logActivity(
                    username,
                    username,
                    "store_inventory_batch_add",
                    "store_inventory_addition",
                    String.format("Added %d units of %s to store %s (batch operation)",
                        item.getQuantity(), product.getName(), store.getStoreName())
                );
                
                logger.debug("✅ Added {} units of {} (ID: {})", 
                    item.getQuantity(), product.getName(), product.getId());
                
                // Add to successful additions
                response.getAdditions().add(new StoreInventoryAdditionResponse(addition));
                successCount++;
                
            } catch (Exception e) {
                logger.error("❌ Failed to add product {}: {}", 
                    item.getProductId(), e.getMessage());
                
                // Add to errors (productName extracted earlier to avoid extra DB call)
                response.getErrors().add(new StoreInventoryBatchAddResponse.BatchError(
                    item.getProductId().toString(),
                    productName,
                    e.getMessage()
                ));
                failCount++;
            }
        }
        
        response.setSuccessfulItems(successCount);
        response.setFailedItems(failCount);
        
        logger.info("✅ Batch restock completed: {} successful, {} failed out of {} total",
            successCount, failCount, request.getItems().size());
        
        return response;
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

    /**
     * Withdraw inventory from store (manual withdrawal for damages, losses, etc.)
     */
    @Transactional
    public StoreInventoryWithdrawalResponse withdrawInventory(StoreInventoryWithdrawalRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get store and product
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + request.getStoreId()));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Verify product belongs to store
        if (product.getStore() == null || !product.getStore().getId().equals(store.getId())) {
            throw new IllegalArgumentException("Product does not belong to the specified store");
        }

        // Check sufficient inventory
        Integer currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
        if (currentQty < request.getQuantity()) {
            throw new IllegalArgumentException(String.format(
                "Insufficient inventory. Available: %d, Requested: %d", currentQty, request.getQuantity()));
        }

        // Create withdrawal record
        StoreInventoryWithdrawal withdrawal = new StoreInventoryWithdrawal();
        withdrawal.setStore(store);
        withdrawal.setProduct(product);
        withdrawal.setQuantity(request.getQuantity());
        withdrawal.setReferenceNumber(request.getReferenceNumber());
        withdrawal.setWithdrawalDate(request.getWithdrawalDate() != null ? request.getWithdrawalDate() : LocalDate.now());
        withdrawal.setReason(request.getReason());
        withdrawal.setNotes(request.getNotes());
        withdrawal.setTransactionType(request.getTransactionType() != null ? 
            request.getTransactionType() : StoreInventoryWithdrawal.TransactionType.DAMAGE);
        withdrawal.setStatus(StoreInventoryWithdrawal.TransactionStatus.COMPLETED);
        withdrawal.setCreatedBy(username);

        // Deduct from product inventory
        product.setQuantity(currentQty - request.getQuantity());
        productRepository.save(product);

        // Save withdrawal record
        withdrawal = withdrawalRepository.save(withdrawal);

        logger.info("✅ Store inventory withdrawn: Store={}, Product={}, Quantity={}, Type={}", 
            store.getStoreName(), product.getName(), request.getQuantity(), withdrawal.getTransactionType());

        // Log activity
        activityLogService.logActivity(
            username,
            username,
            "WITHDRAW_INVENTORY",
            "store_inventory",
            String.format("Withdrew %d units of %s from store %s (Type: %s)", 
                request.getQuantity(), product.getName(), store.getStoreName(), withdrawal.getTransactionType())
        );

        return new StoreInventoryWithdrawalResponse(withdrawal);
    }

    /**
     * Get withdrawal history for a store
     */
    public Page<StoreInventoryWithdrawal> getWithdrawals(
            UUID storeId,
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String filterByUsername,
            Pageable pageable) {

        // If filtering by user, get withdrawals created by that user
        if (filterByUsername != null) {
            return withdrawalRepository.findByStoreIdAndCreatedBy(storeId, filterByUsername, pageable);
        }

        // If filtering by date range
        if (startDate != null && endDate != null) {
            return Page.empty(pageable); // TODO: Implement date range filtering when needed
        }

        // Otherwise return all for the store
        return withdrawalRepository.findByStoreId(storeId, pageable);
    }
}
