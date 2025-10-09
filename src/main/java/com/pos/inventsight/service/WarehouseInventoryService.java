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

        // Update inventory levels with pessimistic lock for concurrency safety
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseAndProductWithLock(warehouse, product)
            .orElse(new WarehouseInventory(warehouse, product, 0));

        inventory.addStock(request.getQuantity());
        
        if (inventory.getId() == null) {
            inventory.setCreatedBy(username);
        } else {
            inventory.setUpdatedBy(username);
        }

        warehouseInventoryRepository.save(inventory);

        // Check for low stock
        checkLowStock(inventory, authentication);

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

        // Save withdrawal first, then update inventory with lock
        withdrawal = withdrawalRepository.save(withdrawal);

        // Re-fetch inventory with lock for safe concurrent update
        WarehouseInventory lockedInventory = warehouseInventoryRepository
            .findByWarehouseAndProductWithLock(warehouse, product)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found during update"));
        
        lockedInventory.removeStock(request.getQuantity());
        lockedInventory.setUpdatedBy(username);
        warehouseInventoryRepository.save(lockedInventory);

        // Check for low stock
        checkLowStock(lockedInventory, authentication);

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

        // Use pessimistic lock for concurrency-safe release
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductIdWithLock(warehouseId, productId)
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
     * Edit inventory addition (same-day only)
     */
    @Transactional
    public void editAdditionSameDay(UUID additionId, WarehouseInventoryAdditionRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get the addition
        WarehouseInventoryAddition addition = additionRepository.findById(additionId)
            .orElseThrow(() -> new ResourceNotFoundException("Addition not found with ID: " + additionId));

        // Check if same day edit
        LocalDate today = LocalDate.now();
        LocalDate createdDate = addition.getCreatedAt().toLocalDate();
        if (!createdDate.equals(today)) {
            throw new IllegalArgumentException("Can only edit additions created today. This addition was created on " + createdDate);
        }

        // Calculate the difference in quantity to adjust inventory
        int oldQuantity = addition.getQuantity();
        int newQuantity = request.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        // Update addition fields
        addition.setQuantity(newQuantity);
        addition.setUnitCost(request.getUnitCost());
        addition.setSupplierName(request.getSupplierName());
        addition.setReferenceNumber(request.getReferenceNumber());
        addition.setReceiptDate(request.getReceiptDate() != null ? request.getReceiptDate() : addition.getReceiptDate());
        addition.setExpiryDate(request.getExpiryDate());
        addition.setBatchNumber(request.getBatchNumber());
        addition.setNotes(request.getNotes());
        if (request.getTransactionType() != null) {
            addition.setTransactionType(request.getTransactionType());
        }
        addition.setUpdatedBy(username);

        additionRepository.save(addition);

        // Adjust inventory levels if quantity changed (with lock for concurrency safety)
        if (quantityDiff != 0) {
            WarehouseInventory inventory = warehouseInventoryRepository
                .findByWarehouseAndProductWithLock(addition.getWarehouse(), addition.getProduct())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

            if (quantityDiff > 0) {
                inventory.addStock(quantityDiff);
            } else {
                // Removing stock
                int absQuantityDiff = Math.abs(quantityDiff);
                if (inventory.getAvailableQuantity() < absQuantityDiff) {
                    throw new IllegalArgumentException("Cannot reduce addition quantity: insufficient available stock");
                }
                inventory.removeStock(absQuantityDiff);
            }
            inventory.setUpdatedBy(username);
            warehouseInventoryRepository.save(inventory);

            // Check for low stock
            checkLowStock(inventory, authentication);
        }

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "inventory_addition_edited",
            "warehouse_inventory",
            String.format("Edited addition for %s in warehouse %s (quantity changed from %d to %d)", 
                addition.getProduct().getName(), addition.getWarehouse().getName(), oldQuantity, newQuantity)
        );
    }

    /**
     * Edit inventory withdrawal (same-day only)
     */
    @Transactional
    public void editWithdrawalSameDay(UUID withdrawalId, WarehouseInventoryWithdrawalRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String username = authentication.getName();

        // Get the withdrawal
        WarehouseInventoryWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
            .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found with ID: " + withdrawalId));

        // Check if same day edit
        LocalDate today = LocalDate.now();
        LocalDate createdDate = withdrawal.getCreatedAt().toLocalDate();
        if (!createdDate.equals(today)) {
            throw new IllegalArgumentException("Can only edit withdrawals created today. This withdrawal was created on " + createdDate);
        }

        // Calculate the difference in quantity to adjust inventory
        int oldQuantity = withdrawal.getQuantity();
        int newQuantity = request.getQuantity();
        int quantityDiff = newQuantity - oldQuantity;

        // Update withdrawal fields
        withdrawal.setQuantity(newQuantity);
        withdrawal.setUnitCost(request.getUnitCost());
        withdrawal.setDestination(request.getDestination());
        withdrawal.setReferenceNumber(request.getReferenceNumber());
        withdrawal.setWithdrawalDate(request.getWithdrawalDate() != null ? request.getWithdrawalDate() : withdrawal.getWithdrawalDate());
        withdrawal.setReason(request.getReason());
        withdrawal.setNotes(request.getNotes());
        if (request.getTransactionType() != null) {
            withdrawal.setTransactionType(request.getTransactionType());
        }
        withdrawal.setUpdatedBy(username);

        withdrawalRepository.save(withdrawal);

        // Adjust inventory levels if quantity changed (with lock for concurrency safety)
        if (quantityDiff != 0) {
            WarehouseInventory inventory = warehouseInventoryRepository
                .findByWarehouseAndProductWithLock(withdrawal.getWarehouse(), withdrawal.getProduct())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

            if (quantityDiff > 0) {
                // Withdrawing more - need to check available stock
                if (inventory.getAvailableQuantity() < quantityDiff) {
                    throw new InsufficientStockException(
                        String.format("Insufficient stock. Requested additional: %d, Available: %d", 
                            quantityDiff, inventory.getAvailableQuantity())
                    );
                }
                inventory.removeStock(quantityDiff);
            } else {
                // Withdrawing less - return stock to inventory
                int absQuantityDiff = Math.abs(quantityDiff);
                inventory.addStock(absQuantityDiff);
            }
            inventory.setUpdatedBy(username);
            warehouseInventoryRepository.save(inventory);

            // Check for low stock
            checkLowStock(inventory, authentication);
        }

        // Log activity
        activityLogService.logActivity(
            authentication.getName(),
            username,
            "inventory_withdrawal_edited",
            "warehouse_inventory",
            String.format("Edited withdrawal for %s from warehouse %s (quantity changed from %d to %d)", 
                withdrawal.getProduct().getName(), withdrawal.getWarehouse().getName(), oldQuantity, newQuantity)
        );
    }

    /**
     * List inventory additions with filters
     */
    public List<WarehouseInventoryAddition> listAdditions(UUID warehouseId, LocalDate startDate, LocalDate endDate, String transactionType) {
        if (startDate != null && endDate != null) {
            if (transactionType != null) {
                try {
                    WarehouseInventoryAddition.TransactionType type = WarehouseInventoryAddition.TransactionType.valueOf(transactionType);
                    return additionRepository.findByWarehouseIdAndReceiptDateBetween(warehouseId, startDate, endDate)
                        .stream()
                        .filter(a -> a.getTransactionType() == type)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
                }
            }
            return additionRepository.findByWarehouseIdAndReceiptDateBetween(warehouseId, startDate, endDate);
        } else if (transactionType != null) {
            try {
                WarehouseInventoryAddition.TransactionType type = WarehouseInventoryAddition.TransactionType.valueOf(transactionType);
                return additionRepository.findByWarehouseId(warehouseId)
                    .stream()
                    .filter(a -> a.getTransactionType() == type)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
            }
        } else {
            return additionRepository.findByWarehouseIdOrderByReceiptDateDesc(warehouseId);
        }
    }

    /**
     * List inventory withdrawals with filters
     */
    public List<WarehouseInventoryWithdrawal> listWithdrawals(UUID warehouseId, LocalDate startDate, LocalDate endDate, String transactionType) {
        if (startDate != null && endDate != null) {
            if (transactionType != null) {
                try {
                    WarehouseInventoryWithdrawal.TransactionType type = WarehouseInventoryWithdrawal.TransactionType.valueOf(transactionType);
                    return withdrawalRepository.findByWarehouseIdAndWithdrawalDateBetween(warehouseId, startDate, endDate)
                        .stream()
                        .filter(w -> w.getTransactionType() == type)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
                }
            }
            return withdrawalRepository.findByWarehouseIdAndWithdrawalDateBetween(warehouseId, startDate, endDate);
        } else if (transactionType != null) {
            try {
                WarehouseInventoryWithdrawal.TransactionType type = WarehouseInventoryWithdrawal.TransactionType.valueOf(transactionType);
                return withdrawalRepository.findByWarehouseId(warehouseId)
                    .stream()
                    .filter(w -> w.getTransactionType() == type)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
            }
        } else {
            return withdrawalRepository.findByWarehouseIdOrderByWithdrawalDateDesc(warehouseId);
        }
    }

    /**
     * Check for low stock and log alert if needed
     */
    private void checkLowStock(WarehouseInventory inventory, Authentication authentication) {
        if (inventory.isLowStock()) {
            activityLogService.logActivity(
                authentication.getName(),
                authentication.getName(),
                "low_stock_alert",
                "warehouse_inventory",
                String.format("LOW STOCK ALERT: %s in warehouse %s - Available: %d, Reorder Point: %d",
                    inventory.getProduct().getName(),
                    inventory.getWarehouse().getName(),
                    inventory.getAvailableQuantity(),
                    inventory.getReorderPoint())
            );
        }
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