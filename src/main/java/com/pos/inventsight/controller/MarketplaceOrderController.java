package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.service.MarketplaceOrderService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/marketplace/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MarketplaceOrderController {
    
    @Autowired
    private MarketplaceOrderService marketplaceOrderService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    /**
     * Helper method to get user's company
     */
    private Company getUserCompany(User user) {
        List<Company> companies = companyStoreUserRepository.findCompaniesByUser(user);
        if (companies.isEmpty()) {
            return null;
        }
        return companies.get(0); // Return first company
    }
    
    /**
     * POST /api/marketplace/orders - Create order
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody Map<String, Object> orderData,
                                        Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            Store currentStore = userService.getCurrentUserStore();
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            if (currentStore == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "No active store found for current user"));
            }
            
            // Extract data from request
            UUID productAdId = UUID.fromString((String) orderData.get("productAdId"));
            Integer quantity = (Integer) orderData.get("quantity");
            String deliveryAddress = (String) orderData.get("deliveryAddress");
            String notes = (String) orderData.get("notes");
            
            // Create order
            MarketplaceOrder order = new MarketplaceOrder();
            order.setQuantity(quantity);
            order.setDeliveryAddress(deliveryAddress);
            order.setNotes(notes);
            
            MarketplaceOrder created = marketplaceOrderService.createOrder(
                order, company, currentStore, currentUser, productAdId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order placed successfully");
            response.put("orderId", created.getId());
            response.put("totalPrice", created.getTotalPrice());
            response.put("order", created);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create order: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/marketplace/orders - List orders
     */
    @GetMapping
    public ResponseEntity<?> getOrders(@RequestParam(required = false) String role,
                                      @RequestParam(required = false) String status,
                                      Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<MarketplaceOrder> orders;
            
            // Default to buyer orders if role not specified
            if ("seller".equalsIgnoreCase(role)) {
                if (status != null) {
                    MarketplaceOrderStatus orderStatus = MarketplaceOrderStatus.valueOf(status.toUpperCase());
                    orders = marketplaceOrderService.getSellerOrders(company.getId()).stream()
                        .filter(o -> o.getStatus() == orderStatus)
                        .toList();
                } else {
                    orders = marketplaceOrderService.getSellerOrders(company.getId());
                }
            } else {
                if (status != null) {
                    MarketplaceOrderStatus orderStatus = MarketplaceOrderStatus.valueOf(status.toUpperCase());
                    orders = marketplaceOrderService.getBuyerOrders(company.getId()).stream()
                        .filter(o -> o.getStatus() == orderStatus)
                        .toList();
                } else {
                    orders = marketplaceOrderService.getBuyerOrders(company.getId());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("count", orders.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch orders: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/marketplace/orders/{id} - Get order details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable UUID id) {
        try {
            MarketplaceOrder order = marketplaceOrderService.getOrderById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch order: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/marketplace/orders/{id}/confirm - Confirm order (seller)
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable UUID id) {
        try {
            MarketplaceOrder confirmed = marketplaceOrderService.confirmOrder(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order confirmed");
            response.put("order", confirmed);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to confirm order: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/marketplace/orders/{id}/ship - Mark as shipped
     */
    @PutMapping("/{id}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable UUID id) {
        try {
            MarketplaceOrder shipped = marketplaceOrderService.shipOrder(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order marked as shipped");
            response.put("order", shipped);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to ship order: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/marketplace/orders/{id}/deliver - Mark as delivered
     */
    @PutMapping("/{id}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable UUID id) {
        try {
            MarketplaceOrder delivered = marketplaceOrderService.deliverOrder(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order marked as delivered");
            response.put("order", delivered);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to deliver order: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/marketplace/orders/{id}/cancel - Cancel order
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID id,
                                        @RequestBody(required = false) Map<String, String> requestData) {
        try {
            String reason = requestData != null ? requestData.get("reason") : null;
            MarketplaceOrder cancelled = marketplaceOrderService.cancelOrder(id, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled");
            response.put("order", cancelled);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to cancel order: " + e.getMessage()));
        }
    }
}
