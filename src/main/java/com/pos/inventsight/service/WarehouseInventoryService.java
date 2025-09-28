package com.pos.inventsight.service;

import com.pos.inventsight.dto.WarehouseInventoryAdditionRequest;
import com.pos.inventsight.dto.WarehouseInventoryRequest;
import com.pos.inventsight.dto.WarehouseInventoryResponse;
import com.pos.inventsight.dto.WarehouseInventoryWithdrawalRequest;
import com.pos.inventsight.exception.InsufficientStockException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventory;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryAdditionRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryRepository;
import com.pos.inventsight.repository.sql.WarehouseInventoryWithdrawalRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing warehouse inventory
 */
@Service
@Transactional
public class WarehouseInventoryService {

    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Autowired
    private WarehouseInventoryAdditionRepository additionRepository;

    @Autowired
    private WarehouseInventoryWithdrawalRepository withdrawalRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ActivityLogService activityLogService;

    /**
     * Get or create warehouse inventory record
     */
    public WarehouseInventoryResponse getOrCreateWarehouseInventory(WarehouseInventoryRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get warehouse and product
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + request.getWarehouseId()));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Find or create inventory record
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseAndProduct(warehouse, product)
            .orElse(new WarehouseInventory(warehouse, product, 0));

        // Update inventory fields
        inventory.setCurrentQuantity(request.getCurrentQuantity());
        inventory.setReservedQuantity(request.getReservedQuantity());
        inventory.setMinimumStockLevel(request.getMinimumStockLevel());
        inventory.setMaximumStockLevel(request.getMaximumStockLevel());
        inventory.setReorderPoint(request.getReorderPoint());
        inventory.setLocationInWarehouse(request.getLocationInWarehouse());

        if (inventory.getId() == null) {
            inventory.setCreatedBy(username);
        } else {
            inventory.setUpdatedBy(username);
        }

        inventory = warehouseInventoryRepository.save(inventory);

        return convertToResponse(inventory);
    }

    /**
     * Add inventory to warehouse
     */
    @Transactional
    public void addInventory(WarehouseInventoryAdditionRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get warehouse and product
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + request.getWarehouseId()));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Create addition record
        WarehouseInventoryAddition addition = new WarehouseInventoryAddition(warehouse, product, request.getQuantity());
        addition.setUnitCost(request.getUnitCost());
        addition.setSupplierName(request.getSupplierName());
        addition.setReferenceNumber(request.getReferenceNumber());
        addition.setReceiptDate(request.getReceiptDate() != null ? request.getReceiptDate() : LocalDate.now());
        addition.setExpiryDate(request.getExpiryDate());
        addition.setBatchNumber(request.getBatchNumber());
        addition.setNotes(request.getNotes());
        addition.setTransactionType(request.getTransactionType() != null ? 
            request.getTransactionType() : WarehouseInventoryAddition.TransactionType.RECEIPT);
        addition.setCreatedBy(username);

        addition = additionRepository.save(addition);

        // Update inventory levels
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseAndProduct(warehouse, product)
            .orElse(new WarehouseInventory(warehouse, product, 0));

        inventory.addStock(request.getQuantity());
        
        if (inventory.getId() == null) {
            inventory.setCreatedBy(username);
        } else {
            inventory.setUpdatedBy(username);
        }

        warehouseInventoryRepository.save(inventory);

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "inventory_added",
            "warehouse_inventory",
            String.format("Added %d units of %s to warehouse %s", 
                request.getQuantity(), product.getName(), warehouse.getName())
        );
    }

    /**
     * Withdraw inventory from warehouse
     */
    @Transactional
    public void withdrawInventory(WarehouseInventoryWithdrawalRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get warehouse and product
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + request.getWarehouseId()));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Get current inventory
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseAndProduct(warehouse, product)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory found for this product in the warehouse"));

        // Check if sufficient stock is available
        if (!inventory.canReserve(request.getQuantity())) {
            throw new InsufficientStockException(
                String.format("Insufficient stock. Requested: %d, Available: %d", 
                    request.getQuantity(), inventory.getAvailableQuantity())
            );
        }

        // Create withdrawal record
        WarehouseInventoryWithdrawal withdrawal = new WarehouseInventoryWithdrawal(
            warehouse, product, request.getQuantity(), request.getReason());
        withdrawal.setUnitCost(request.getUnitCost());
        withdrawal.setDestination(request.getDestination());
        withdrawal.setReferenceNumber(request.getReferenceNumber());
        withdrawal.setWithdrawalDate(request.getWithdrawalDate() != null ? 
            request.getWithdrawalDate() : LocalDate.now());
        withdrawal.setNotes(request.getNotes());
        withdrawal.setTransactionType(request.getTransactionType() != null ? 
            request.getTransactionType() : WarehouseInventoryWithdrawal.TransactionType.ISSUE);
        withdrawal.setCreatedBy(username);

        withdrawal = withdrawalRepository.save(withdrawal);

        // Update inventory levels
        inventory.removeStock(request.getQuantity());
        inventory.setUpdatedBy(username);
        warehouseInventoryRepository.save(inventory);

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "inventory_withdrawn",
            "warehouse_inventory",
            String.format("Withdrawn %d units of %s from warehouse %s", 
                request.getQuantity(), product.getName(), warehouse.getName())
        );
    }

    /**
     * Reserve inventory
     */
    @Transactional
    public void reserveInventory(UUID warehouseId, UUID productId, Integer quantity, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory found for this product in the warehouse"));

        try {
            inventory.reserveQuantity(quantity);
            inventory.setUpdatedBy(username);
            warehouseInventoryRepository.save(inventory);

            // Log activity
            activityLogService.logActivity(
                authentication.getName(),
                username,
                "inventory_reserved",
                "warehouse_inventory",
                String.format("Reserved %d units in warehouse %s", 
                    quantity, inventory.getWarehouse().getName())
            );
        } catch (IllegalArgumentException e) {
            throw new InsufficientStockException(e.getMessage());
        }
    }

    /**
     * Release reservation
     */
    @Transactional
    public void releaseReservation(UUID warehouseId, UUID productId, Integer quantity, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory found for this product in the warehouse"));

        try {
            inventory.releaseReservation(quantity);
            inventory.setUpdatedBy(username);
            warehouseInventoryRepository.save(inventory);

            // Log activity
            activityLogService.logActivity(
                authentication.getName(),
                username,
                "inventory_reservation_released",
                "warehouse_inventory",
                String.format("Released %d units reservation in warehouse %s", 
                    quantity, inventory.getWarehouse().getName())
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Get inventory for a warehouse
     */
    public List<WarehouseInventoryResponse> getWarehouseInventory(UUID warehouseId) {
        return warehouseInventoryRepository.findByWarehouseId(warehouseId)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get inventory for a product across all warehouses
     */
    public List<WarehouseInventoryResponse> getProductInventory(UUID productId) {
        return warehouseInventoryRepository.findByProductId(productId)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get low stock items
     */
    public List<WarehouseInventoryResponse> getLowStockItems() {
        return warehouseInventoryRepository.findLowStockItems()
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get low stock items in specific warehouse
     */
    public List<WarehouseInventoryResponse> getLowStockItemsByWarehouse(UUID warehouseId) {
        return warehouseInventoryRepository.findLowStockItemsByWarehouse(warehouseId)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Search inventory by product name or SKU
     */
    public List<WarehouseInventoryResponse> searchInventory(String searchTerm) {
        return warehouseInventoryRepository.searchByProductNameOrSku(searchTerm)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get total inventory value for warehouse
     */
    public Double getTotalInventoryValue(UUID warehouseId) {
        return warehouseInventoryRepository.getTotalInventoryValueByWarehouse(warehouseId);
    }

    /**
     * Convert entity to response DTO
     */
    private WarehouseInventoryResponse convertToResponse(WarehouseInventory inventory) {
        WarehouseInventoryResponse response = new WarehouseInventoryResponse();
        response.setId(inventory.getId());
        response.setWarehouseId(inventory.getWarehouse().getId());
        response.setWarehouseName(inventory.getWarehouse().getName());
        response.setProductId(inventory.getProduct().getId());
        response.setProductName(inventory.getProduct().getName());
        response.setProductSku(inventory.getProduct().getSku());
        response.setCurrentQuantity(inventory.getCurrentQuantity());
        response.setReservedQuantity(inventory.getReservedQuantity());
        response.setAvailableQuantity(inventory.getAvailableQuantity());
        response.setMinimumStockLevel(inventory.getMinimumStockLevel());
        response.setMaximumStockLevel(inventory.getMaximumStockLevel());
        response.setReorderPoint(inventory.getReorderPoint());
        response.setLocationInWarehouse(inventory.getLocationInWarehouse());
        response.setLastUpdated(inventory.getLastUpdated());
        response.setCreatedAt(inventory.getCreatedAt());
        response.setUpdatedAt(inventory.getUpdatedAt());
        response.setCreatedBy(inventory.getCreatedBy());
        response.setUpdatedBy(inventory.getUpdatedBy());
        response.setLowStock(inventory.isLowStock());
        response.setOverstock(inventory.isOverstock());
        return response;
    }
}