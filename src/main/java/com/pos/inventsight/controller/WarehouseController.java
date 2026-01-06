package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.WarehouseRequest;
import com.pos.inventsight.dto.WarehouseResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.security.RoleConstants;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for warehouse management
 */
@RestController
@RequestMapping("/warehouses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WarehouseController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseController.class);

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private ProductRepository productRepository;

    /**
     * Create a new warehouse (GM+ only)
     * ✅ UPDATED: Uses RoleConstants.CAN_MANAGE_WAREHOUSES
     */
    @PostMapping
    @PreAuthorize(RoleConstants.CAN_MANAGE_WAREHOUSES)
    public ResponseEntity<?> createWarehouse(@Valid @RequestBody WarehouseRequest request,
                                           BindingResult bindingResult,
                                           Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "unknown";
        
        // Log incoming request
        System.out.println("➕ InventSight - Creating warehouse");
        System.out.println("   User: " + username);
        System.out.println("   Name: " + request.getName());
        System.out.println("   Location: " + request.getLocation());
        System.out.println("   Description: " + request.getDescription());
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            System.err.println("❌ Validation errors:");
            
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
                System.err.println("   - " + fieldName + ": " + errorMessage);
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Validation failed");
            errorResponse.put("errors", errors);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        try {
            User user = userService.getUserByUsername(username);
            
            // ✅ Additional check for GM+ level
            if (!isGMPlusRole(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Only General Managers and above can create warehouses"));
            }
            
            WarehouseResponse warehouse = warehouseService.createWarehouse(request, authentication);
            
            System.out.println("✅ Warehouse created: " + warehouse.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Warehouse created successfully");
            response.put("data", warehouse);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error creating warehouse: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error creating warehouse: " + e.getMessage()));
        }
    }

    /**
     * Get all warehouses (all authenticated users can view)
     * GET /api/warehouses
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllWarehouses(Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            System.out.println("🏢 InventSight - Getting warehouses for user: " + username);
            
            List<WarehouseResponse> warehouses = warehouseService.getAllActiveWarehouses();
            
            System.out.println("✅ Returning " + warehouses.size() + " warehouses");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouses);
            response.put("message", "Warehouses retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching warehouses: " + e.getMessage()));
        }
    }

    /**
     * Get warehouse by ID
     * GET /api/warehouses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWarehouseById(@PathVariable UUID id, Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            System.out.println("🏢 InventSight - Getting warehouse " + id + " for user: " + username);
            
            WarehouseResponse warehouse = warehouseService.getWarehouseById(id);
            
            System.out.println("✅ Returning warehouse: " + warehouse.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "Warehouse retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Warehouse not found: " + e.getMessage()));
        }
    }

    /**
     * Update warehouse (GM+ only)
     * ✅ UPDATED: Uses RoleConstants.CAN_MANAGE_WAREHOUSES
     */
    @PutMapping("/{id}")
    @PreAuthorize(RoleConstants.CAN_MANAGE_WAREHOUSES)
    public ResponseEntity<?> updateWarehouse(@PathVariable UUID id,
                                           @Valid @RequestBody WarehouseRequest request,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            if (!isGMPlusRole(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Only General Managers and above can update warehouses"));
            }
            
            WarehouseResponse warehouse = warehouseService.updateWarehouse(id, request, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Warehouse updated successfully");
            response.put("warehouse", warehouse);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error updating warehouse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error updating warehouse: " + e.getMessage()));
        }
    }

    /**
     * Delete warehouse (deactivate) (GM+ only)
     * ✅ UPDATED: Uses RoleConstants.CAN_MANAGE_WAREHOUSES
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.CAN_MANAGE_WAREHOUSES)
    public ResponseEntity<?> deleteWarehouse(@PathVariable UUID id,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            if (!isGMPlusRole(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Only General Managers and above can delete warehouses"));
            }
            
            warehouseService.deleteWarehouse(id, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Warehouse deactivated successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting warehouse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error deleting warehouse: " + e.getMessage()));
        }
    }

    /**
     * Search warehouses
     * GET /api/warehouses/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchWarehouses(@RequestParam("q") String searchTerm) {
        try {
            List<WarehouseResponse> warehouses = warehouseService.searchWarehouses(searchTerm);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouses", warehouses);
            response.put("count", warehouses.size());
            response.put("searchTerm", searchTerm);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching warehouses: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error searching warehouses: " + e.getMessage()));
        }
    }

    /**
     * Get warehouses by type
     * GET /api/warehouses/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getWarehousesByType(@PathVariable String type) {
        try {
            Warehouse.WarehouseType warehouseType = Warehouse.WarehouseType.valueOf(type.toUpperCase());
            List<WarehouseResponse> warehouses = warehouseService.getWarehousesByType(warehouseType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouses", warehouses);
            response.put("count", warehouses.size());
            response.put("type", warehouseType);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid warehouse type: " + type));
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouses by type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching warehouses: " + e.getMessage()));
        }
    }

    /**
     * Get warehouses by city
     * GET /api/warehouses/city/{city}
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<?> getWarehousesByCity(@PathVariable String city) {
        try {
            List<WarehouseResponse> warehouses = warehouseService.getWarehousesByCity(city);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouses", warehouses);
            response.put("count", warehouses.size());
            response.put("city", city);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouses by city: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching warehouses: " + e.getMessage()));
        }
    }

    /**
     * Get warehouse statistics
     * GET /api/warehouses/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getWarehouseStats() {
        try {
            long activeCount = warehouseService.getActiveWarehouseCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeWarehouses", activeCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouse stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching statistics: " + e.getMessage()));
        }
    }

    /**
     * Get products available for a specific warehouse
     * These are products created from predefined items that are associated with this warehouse
     * GET /api/warehouses/{warehouseId}/available-products
     */
    @GetMapping("/{warehouseId}/available-products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getWarehouseAvailableProducts(
            @PathVariable UUID warehouseId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("📦 Getting available products for warehouse: {} (user: {})", warehouseId, username);
            
            // Verify warehouse exists
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId));
            
            // Get products associated with this warehouse
            // These come from predefined_item_warehouses → predefined_items → products
            List<Product> products = productRepository.findByWarehouseId(warehouseId);
            
            logger.info("✅ Found {} products for warehouse {} ({})", 
                products.size(), warehouse.getName(), warehouseId);
            
            // Log each product for debugging
            if (logger.isDebugEnabled()) {
                products.forEach(p -> 
                    logger.debug("  - Product: {} (SKU: {}, ID: {})", p.getName(), p.getSku(), p.getId())
                );
            }
            
            // Convert to response DTOs
            List<Map<String, Object>> productList = products.stream()
                .map(product -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("id", product.getId());
                    productMap.put("name", product.getName());
                    productMap.put("sku", product.getSku());
                    productMap.put("description", product.getDescription());
                    productMap.put("category", product.getCategory());
                    productMap.put("unitType", product.getUnit());
                    productMap.put("price", product.getRetailPrice());
                    productMap.put("warehouseId", warehouseId); // ✅ Include warehouse ID
                    productMap.put("predefinedItemId", product.getPredefinedItem() != null ? 
                                   product.getPredefinedItem().getId() : null);
                    return productMap;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("warehouseName", warehouse.getName());
            response.put("products", productList);
            response.put("count", productList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            logger.error("❌ Warehouse not found: {}", warehouseId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        } catch (Exception e) {
            logger.error("❌ Error fetching available products for warehouse {}: {}", warehouseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Error fetching available products: " + e.getMessage()
                ));
        }
    }

    /**
     * Helper method to check if user is GM+ level
     */
    private boolean isGMPlusRole(UserRole role) {
        return com.pos.inventsight.constants.RoleConstants.isGMPlus(role);
    }
}