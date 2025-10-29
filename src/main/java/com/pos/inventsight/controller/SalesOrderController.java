package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.IdempotencyService;
import com.pos.inventsight.service.SalesOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for sales order operations
 */
@RestController
@RequestMapping("/sales/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SalesOrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderController.class);
    
    @Autowired
    private SalesOrderService salesOrderService;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    /**
     * Create a new sales order
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateSalesOrderRequest request,
            Authentication authentication) {
        try {
            UUID tenantId = idempotencyService.getCurrentTenantId();
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Tenant context is required"));
            }
            
            SalesOrder order = salesOrderService.createOrder(
                tenantId,
                request.getCurrencyCode(),
                request.getCustomerName(),
                request.getCustomerPhone(),
                authentication
            );
            
            SalesOrderResponse response = mapToResponse(order);
            logger.info("Created order {} for tenant {}", order.getId(), tenantId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create order: " + e.getMessage()));
        }
    }
    
    /**
     * Add item to order (reserves stock)
     */
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> addItem(
            @PathVariable UUID orderId,
            @Valid @RequestBody AddSalesOrderItemRequest request,
            Authentication authentication) {
        try {
            // Determine if user is employee (non-manager)
            boolean isEmployee = !hasManagerRole(authentication);
            
            SalesOrderItem item = salesOrderService.addItem(
                orderId,
                request.getWarehouseId(),
                request.getProductId(),
                request.getQuantity(),
                request.getUnitPrice(),
                request.getDiscountPercent(),
                request.getCurrencyCode(),
                isEmployee
            );
            
            SalesOrderItemResponse response = mapItemToResponse(item);
            logger.info("Added item {} to order {}", item.getId(), orderId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (NoSuchElementException e) {
            logger.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding item to order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to add item: " + e.getMessage()));
        }
    }
    
    /**
     * Submit order for processing
     */
    @PostMapping("/{orderId}/submit")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> submitOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        try {
            SalesOrder order = salesOrderService.submit(orderId, authentication);
            SalesOrderResponse response = mapToResponse(order);
            
            logger.info("Submitted order {}, status: {}", orderId, order.getStatus());
            return ResponseEntity.ok(response);
            
        } catch (NoSuchElementException e) {
            logger.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Invalid order state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to submit order: " + e.getMessage()));
        }
    }
    
    /**
     * Request order cancellation
     */
    @PostMapping("/{orderId}/cancel-request")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> requestCancel(
            @PathVariable UUID orderId,
            Authentication authentication) {
        try {
            SalesOrder order = salesOrderService.requestCancel(orderId, authentication);
            SalesOrderResponse response = mapToResponse(order);
            
            logger.info("Cancel requested for order {}, status: {}", orderId, order.getStatus());
            return ResponseEntity.ok(response);
            
        } catch (NoSuchElementException e) {
            logger.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error requesting cancel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to request cancel: " + e.getMessage()));
        }
    }
    
    /**
     * Manager approves order
     */
    @PostMapping("/{orderId}/approve")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER')")
    public ResponseEntity<?> approveOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        try {
            SalesOrder order = salesOrderService.approve(orderId, authentication);
            SalesOrderResponse response = mapToResponse(order);
            
            logger.info("Order {} approved by manager", orderId);
            return ResponseEntity.ok(response);
            
        } catch (NoSuchElementException e) {
            logger.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Unauthorized approval attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Invalid order state for approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to approve order: " + e.getMessage()));
        }
    }
    
    /**
     * Manager approves cancellation
     */
    @PostMapping("/{orderId}/cancel-approve")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER')")
    public ResponseEntity<?> approveCancel(
            @PathVariable UUID orderId,
            Authentication authentication) {
        try {
            SalesOrder order = salesOrderService.approveCancel(orderId, authentication);
            SalesOrderResponse response = mapToResponse(order);
            
            logger.info("Order {} cancellation approved by manager", orderId);
            return ResponseEntity.ok(response);
            
        } catch (NoSuchElementException e) {
            logger.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Unauthorized cancel approval attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Invalid order state for cancel approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving cancellation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to approve cancellation: " + e.getMessage()));
        }
    }
    
    /**
     * Get order details
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        try {
            UUID tenantId = idempotencyService.getCurrentTenantId();
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Tenant context is required"));
            }
            
            Optional<SalesOrder> orderOpt = salesOrderService.getOrder(orderId, tenantId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Order not found"));
            }
            
            SalesOrderResponse response = mapToResponse(orderOpt.get());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch order: " + e.getMessage()));
        }
    }
    
    // Helper methods
    
    private SalesOrderResponse mapToResponse(SalesOrder order) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setId(order.getId());
        response.setTenantId(order.getTenantId());
        response.setStatus(order.getStatus().name());
        response.setRequiresManagerApproval(order.getRequiresManagerApproval());
        response.setCurrencyCode(order.getCurrencyCode());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setCreatedBy(order.getCreatedBy());
        response.setUpdatedBy(order.getUpdatedBy());
        
        List<SalesOrderItemResponse> items = order.getItems().stream()
            .map(this::mapItemToResponse)
            .collect(Collectors.toList());
        response.setItems(items);
        
        // Calculate total amount
        BigDecimal total = items.stream()
            .map(SalesOrderItemResponse::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(total);
        
        return response;
    }
    
    private SalesOrderItemResponse mapItemToResponse(SalesOrderItem item) {
        SalesOrderItemResponse response = new SalesOrderItemResponse();
        response.setId(item.getId());
        response.setWarehouseId(item.getWarehouse().getId());
        response.setWarehouseName(item.getWarehouse().getName());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setProductSku(item.getProduct().getSku());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setDiscountPercent(item.getDiscountPercent());
        response.setCurrencyCode(item.getCurrencyCode());
        response.setLineTotal(item.getLineTotal());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
    
    private boolean hasManagerRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> {
                String role = auth.getAuthority();
                return role.equals("ROLE_FOUNDER") || 
                       role.equals("ROLE_GENERAL_MANAGER") || 
                       role.equals("ROLE_STORE_MANAGER");
            });
    }
}
