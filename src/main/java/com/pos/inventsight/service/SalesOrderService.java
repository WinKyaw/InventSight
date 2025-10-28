package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import com.pos.inventsight.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing sales orders created by employees
 */
@Service
public class SalesOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderService.class);
    
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    
    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;
    
    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SyncChangeService syncChangeService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Value("${inventsight.sales.enabled:true}")
    private boolean salesEnabled;
    
    @Value("${inventsight.sales.max-employee-discount-percent:10}")
    private double maxEmployeeDiscountPercent;
    
    @Value("${inventsight.sales.cross-store.employee-requires-approval:true}")
    private boolean crossStoreRequiresApproval;
    
    /**
     * Create a new sales order
     */
    @Transactional
    public SalesOrder createOrder(UUID tenantId, String currencyCode, String customerName, 
                                  String customerPhone, Authentication auth) {
        if (!salesEnabled) {
            throw new IllegalStateException("Sales functionality is not enabled");
        }
        
        String username = auth != null ? auth.getName() : "system";
        
        SalesOrder order = new SalesOrder(tenantId, currencyCode, username);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        
        order = salesOrderRepository.save(order);
        
        // Emit sync change event
        syncChangeService.recordChange("SalesOrder", order.getId().toString(), "INSERT", 
            Map.of("orderId", order.getId(), "status", order.getStatus(), "tenantId", tenantId));
        
        logger.info("Created sales order {} for tenant {}", order.getId(), tenantId);
        return order;
    }
    
    /**
     * Add item to order and reserve stock
     */
    @Transactional
    public SalesOrderItem addItem(UUID orderId, UUID warehouseId, UUID productId, 
                                  Integer quantity, BigDecimal unitPrice, 
                                  BigDecimal discountPercent, String currencyCode, 
                                  boolean isEmployee) {
        if (!salesEnabled) {
            throw new IllegalStateException("Sales functionality is not enabled");
        }
        
        // Validate inputs
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be non-negative");
        }
        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        
        // Load order
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        
        if (!order.getStatus().isModifiable()) {
            throw new IllegalStateException("Order cannot be modified in status: " + order.getStatus());
        }
        
        // Load warehouse and product
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new NoSuchElementException("Warehouse not found: " + warehouseId));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        
        // Reserve inventory with pessimistic lock to prevent overselling
        WarehouseInventory inventory = warehouseInventoryRepository
            .findByWarehouseIdAndProductIdWithLock(warehouseId, productId)
            .orElseThrow(() -> new NoSuchElementException(
                "No inventory found for product " + productId + " in warehouse " + warehouseId));
        
        // Check if enough stock is available
        if (!inventory.canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock. Available: " + 
                inventory.getAvailableQuantity() + ", Requested: " + quantity);
        }
        
        // Reserve the stock
        inventory.reserveQuantity(quantity);
        warehouseInventoryRepository.save(inventory);
        
        // Create order item
        SalesOrderItem item = new SalesOrderItem(order, warehouse, product, 
            quantity, unitPrice, currencyCode);
        item.setDiscountPercent(discountPercent);
        item = salesOrderItemRepository.save(item);
        
        // Check if manager approval is required
        boolean needsApproval = false;
        
        // Check discount threshold for employees
        if (isEmployee && discountPercent.compareTo(new BigDecimal(maxEmployeeDiscountPercent)) > 0) {
            needsApproval = true;
            logger.info("Order {} requires manager approval: employee discount {} exceeds threshold {}", 
                orderId, discountPercent, maxEmployeeDiscountPercent);
        }
        
        // Check cross-store policy (if order has items from multiple warehouses)
        if (crossStoreRequiresApproval && isEmployee) {
            List<SalesOrderItem> existingItems = salesOrderItemRepository.findByOrderId(orderId);
            Set<UUID> warehouses = new HashSet<>();
            warehouses.add(warehouseId);
            for (SalesOrderItem existing : existingItems) {
                warehouses.add(existing.getWarehouse().getId());
            }
            if (warehouses.size() > 1) {
                needsApproval = true;
                logger.info("Order {} requires manager approval: cross-store sourcing detected", orderId);
            }
        }
        
        if (needsApproval) {
            order.setRequiresManagerApproval(true);
            salesOrderRepository.save(order);
        }
        
        // Emit sync change events
        syncChangeService.recordChange("SalesOrderItem", item.getId().toString(), "INSERT", 
            Map.of("orderId", orderId, "productId", productId, "quantity", quantity));
        syncChangeService.recordChange("WarehouseInventory", inventory.getId().toString(), "UPDATE", 
            Map.of("productId", productId, "warehouseId", warehouseId, 
                   "reservedQuantity", inventory.getReservedQuantity()));
        
        logger.info("Added item {} to order {}, reserved {} units", item.getId(), orderId, quantity);
        return item;
    }
    
    /**
     * Submit order for processing
     */
    @Transactional
    public SalesOrder submit(UUID orderId, Authentication auth) {
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        
        if (!order.getStatus().canSubmit()) {
            throw new IllegalStateException("Order cannot be submitted in status: " + order.getStatus());
        }
        
        // Check if order has items
        List<SalesOrderItem> items = salesOrderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot submit empty order");
        }
        
        // Set status based on approval requirement
        if (order.getRequiresManagerApproval()) {
            order.setStatus(OrderStatus.PENDING_MANAGER_APPROVAL);
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        
        String username = auth != null ? auth.getName() : "system";
        order.setUpdatedBy(username);
        order = salesOrderRepository.save(order);
        
        // Emit sync change event
        syncChangeService.recordChange("SalesOrder", order.getId().toString(), "UPDATE", 
            Map.of("orderId", orderId, "status", order.getStatus()));
        
        logger.info("Order {} submitted with status {}", orderId, order.getStatus());
        return order;
    }
    
    /**
     * Request order cancellation
     */
    @Transactional
    public SalesOrder requestCancel(UUID orderId, Authentication auth) {
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        
        String username = auth != null ? auth.getName() : "system";
        order.setUpdatedBy(username);
        
        // If order can be cancelled directly, release reservations and cancel
        if (order.getStatus().canCancelDirectly()) {
            releaseReservations(orderId);
            order.setStatus(OrderStatus.CANCELLED);
            logger.info("Order {} cancelled immediately by {}", orderId, username);
        } else {
            // Otherwise, request manager approval for cancellation
            order.setStatus(OrderStatus.CANCEL_REQUESTED);
            logger.info("Order {} cancellation requested by {}, requires manager approval", orderId, username);
        }
        
        order = salesOrderRepository.save(order);
        
        // Emit sync change event
        syncChangeService.recordChange("SalesOrder", order.getId().toString(), "UPDATE", 
            Map.of("orderId", orderId, "status", order.getStatus()));
        
        return order;
    }
    
    /**
     * Manager approves order (moves from PENDING_MANAGER_APPROVAL to CONFIRMED)
     */
    @Transactional
    public SalesOrder approve(UUID orderId, Authentication auth) {
        if (auth != null && !isManager(auth)) {
            throw new SecurityException("Only managers can approve orders");
        }
        
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING_MANAGER_APPROVAL) {
            throw new IllegalStateException("Order is not pending approval");
        }
        
        order.setStatus(OrderStatus.CONFIRMED);
        String username = auth != null ? auth.getName() : "system";
        order.setUpdatedBy(username);
        order = salesOrderRepository.save(order);
        
        // Emit sync change event
        syncChangeService.recordChange("SalesOrder", order.getId().toString(), "UPDATE", 
            Map.of("orderId", orderId, "status", order.getStatus(), "approvedBy", username));
        
        logger.info("Order {} approved by manager {}", orderId, username);
        return order;
    }
    
    /**
     * Manager approves cancellation (releases reservations and cancels order)
     */
    @Transactional
    public SalesOrder approveCancel(UUID orderId, Authentication auth) {
        if (auth != null && !isManager(auth)) {
            throw new SecurityException("Only managers can approve cancellations");
        }
        
        SalesOrder order = salesOrderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("Order cancellation is not requested");
        }
        
        // Release all reservations
        releaseReservations(orderId);
        
        order.setStatus(OrderStatus.CANCELLED);
        String username = auth != null ? auth.getName() : "system";
        order.setUpdatedBy(username);
        order = salesOrderRepository.save(order);
        
        // Emit sync change event
        syncChangeService.recordChange("SalesOrder", order.getId().toString(), "UPDATE", 
            Map.of("orderId", orderId, "status", order.getStatus(), "cancelledBy", username));
        
        logger.info("Order {} cancellation approved by manager {}", orderId, username);
        return order;
    }
    
    /**
     * Get order by ID with tenant validation
     */
    @Transactional(readOnly = true)
    public Optional<SalesOrder> getOrder(UUID orderId, UUID tenantId) {
        return salesOrderRepository.findByIdAndTenantId(orderId, tenantId);
    }
    
    /**
     * Release reservations for all items in an order
     */
    private void releaseReservations(UUID orderId) {
        List<SalesOrderItem> items = salesOrderItemRepository.findByOrderId(orderId);
        for (SalesOrderItem item : items) {
            WarehouseInventory inventory = warehouseInventoryRepository
                .findByWarehouseIdAndProductIdWithLock(
                    item.getWarehouse().getId(), 
                    item.getProduct().getId())
                .orElse(null);
            
            if (inventory != null) {
                try {
                    inventory.releaseReservation(item.getQuantity());
                    warehouseInventoryRepository.save(inventory);
                    
                    // Emit sync change event
                    syncChangeService.recordChange("WarehouseInventory", inventory.getId().toString(), "UPDATE", 
                        Map.of("productId", item.getProduct().getId(), 
                               "warehouseId", item.getWarehouse().getId(),
                               "reservedQuantity", inventory.getReservedQuantity()));
                    
                    logger.debug("Released reservation of {} units for product {} in warehouse {}", 
                        item.getQuantity(), item.getProduct().getId(), item.getWarehouse().getId());
                } catch (Exception e) {
                    logger.error("Error releasing reservation for item {}: {}", item.getId(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Check if user has manager-level role
     */
    private boolean isManager(Authentication auth) {
        try {
            Object principal = auth.getPrincipal();
            if (!(principal instanceof User)) {
                return false;
            }
            
            User user = (User) principal;
            List<CompanyStoreUser> memberships = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
            
            for (CompanyStoreUser membership : memberships) {
                if (membership.getRole().isManagerLevel()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.debug("Error checking manager status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract tenant ID from current context
     */
    private UUID getCurrentTenantId() {
        String tenantContext = TenantContext.getCurrentTenant();
        if (tenantContext != null && tenantContext.startsWith("company_")) {
            try {
                String uuidStr = tenantContext.substring(8).replace("_", "-");
                return UUID.fromString(uuidStr);
            } catch (Exception e) {
                logger.debug("Failed to parse tenant UUID from context: {}", tenantContext);
            }
        }
        return null;
    }
}
