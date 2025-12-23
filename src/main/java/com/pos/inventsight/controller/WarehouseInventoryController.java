package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.WarehouseInventoryAdditionRequest;
import com.pos.inventsight.dto.WarehouseInventoryRequest;
import com.pos.inventsight.dto.WarehouseInventoryResponse;
import com.pos.inventsight.dto.WarehouseInventoryWithdrawalRequest;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.security.RoleConstants;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.WarehouseInventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for warehouse inventory management with RBAC
 * ✅ UPDATED: Now uses RoleConstants for consistent authorization and supports pagination
 */
@RestController
@RequestMapping("/warehouse-inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WarehouseInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseInventoryController.class);

    @Autowired
    private WarehouseInventoryService warehouseInventoryService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;

    /**
     * Create or update warehouse inventory
     * ✅ UPDATED: Uses RoleConstants.GM_PLUS (includes OWNER)
     */
    @PostMapping
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> createOrUpdateInventory(@Valid @RequestBody WarehouseInventoryRequest request,
                                                   Authentication authentication) {
        try {
            WarehouseInventoryResponse inventory = warehouseInventoryService
                .getOrCreateWarehouseInventory(request, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Warehouse inventory updated successfully");
            response.put("inventory", inventory);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error updating warehouse inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error updating inventory: " + e.getMessage()));
        }
    }

    /**
     * Add inventory to warehouse
     * ✅ UPDATED: Uses RoleConstants.CAN_MODIFY_INVENTORY (includes OWNER)
     */
    @PostMapping("/add")
    @PreAuthorize(RoleConstants.CAN_MODIFY_INVENTORY)
    public ResponseEntity<?> addInventory(@Valid @RequestBody WarehouseInventoryAdditionRequest request,
                                        Authentication authentication) {
        try {
            warehouseInventoryService.addInventory(request, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory added successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error adding inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error adding inventory: " + e.getMessage()));
        }
    }

    /**
     * Withdraw inventory from warehouse
     * ✅ UPDATED: Uses RoleConstants.CAN_WITHDRAW_INVENTORY (includes OWNER)
     */
    @PostMapping("/withdraw")
    @PreAuthorize(RoleConstants.CAN_WITHDRAW_INVENTORY)
    public ResponseEntity<?> withdrawInventory(@Valid @RequestBody WarehouseInventoryWithdrawalRequest request,
                                             Authentication authentication) {
        try {
            warehouseInventoryService.withdrawInventory(request, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory withdrawn successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error withdrawing inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error withdrawing inventory: " + e.getMessage()));
        }
    }

    /**
     * Reserve inventory
     * ✅ UPDATED: Uses RoleConstants.MANAGEMENT
     */
    @PostMapping("/reserve")
    @PreAuthorize(RoleConstants.MANAGEMENT)
    public ResponseEntity<?> reserveInventory(@RequestParam UUID warehouseId,
                                            @RequestParam UUID productId,
                                            @RequestParam Integer quantity,
                                            Authentication authentication) {
        try {
            warehouseInventoryService.reserveInventory(warehouseId, productId, quantity, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory reserved successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error reserving inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error reserving inventory: " + e.getMessage()));
        }
    }

    /**
     * Release inventory reservation
     * ✅ UPDATED: Uses RoleConstants.MANAGEMENT
     */
    @PostMapping("/release")
    @PreAuthorize(RoleConstants.MANAGEMENT)
    public ResponseEntity<?> releaseReservation(@RequestParam UUID warehouseId,
                                               @RequestParam UUID productId,
                                               @RequestParam Integer quantity,
                                               Authentication authentication) {
        try {
            warehouseInventoryService.releaseReservation(warehouseId, productId, quantity, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory reservation released successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error releasing reservation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error releasing reservation: " + e.getMessage()));
        }
    }

    /**
     * Get inventory for a warehouse
     * ✅ UPDATED: Uses RoleConstants.CAN_VIEW_INVENTORY (includes OWNER) and supports pagination
     */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize(RoleConstants.CAN_VIEW_INVENTORY)
    public ResponseEntity<?> getWarehouseInventory(
        @PathVariable UUID warehouseId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            logger.info("📦 Fetching inventory for warehouse: {} (Page: {}, Size: {})", 
                warehouseId, page, size);
            
            Page<WarehouseInventoryResponse> inventory = warehouseInventoryService.getWarehouseInventory(
                warehouseId, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "productName"))
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("inventory", inventory.getContent());
            response.put("currentPage", inventory.getNumber());
            response.put("totalPages", inventory.getTotalPages());
            response.put("totalItems", inventory.getTotalElements());
            response.put("hasMore", inventory.hasNext());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching inventory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Get inventory for a product across all warehouses
     * GET /api/warehouse-inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductInventory(@PathVariable UUID productId) {
        try {
            List<WarehouseInventoryResponse> inventory = warehouseInventoryService.getProductInventory(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inventory", inventory);
            response.put("count", inventory.size());
            response.put("productId", productId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching product inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching inventory: " + e.getMessage()));
        }
    }

    /**
     * Get low stock items
     * GET /api/warehouse-inventory/low-stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockItems(@RequestParam(required = false) UUID warehouseId) {
        try {
            List<WarehouseInventoryResponse> inventory;
            
            if (warehouseId != null) {
                inventory = warehouseInventoryService.getLowStockItemsByWarehouse(warehouseId);
            } else {
                inventory = warehouseInventoryService.getLowStockItems();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inventory", inventory);
            response.put("count", inventory.size());
            if (warehouseId != null) {
                response.put("warehouseId", warehouseId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching low stock items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching low stock items: " + e.getMessage()));
        }
    }

    /**
     * Search inventory
     * GET /api/warehouse-inventory/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchInventory(@RequestParam("q") String searchTerm) {
        try {
            List<WarehouseInventoryResponse> inventory = warehouseInventoryService.searchInventory(searchTerm);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inventory", inventory);
            response.put("count", inventory.size());
            response.put("searchTerm", searchTerm);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error searching inventory: " + e.getMessage()));
        }
    }

    /**
     * Get inventory value for warehouse
     * ✅ UPDATED: Uses RoleConstants.CAN_VIEW_INVENTORY (includes OWNER)
     */
    @GetMapping("/warehouse/{warehouseId}/value")
    @PreAuthorize(RoleConstants.CAN_VIEW_INVENTORY)
    public ResponseEntity<?> getInventoryValue(@PathVariable UUID warehouseId) {
        try {
            Double totalValue = warehouseInventoryService.getTotalInventoryValue(warehouseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalValue", totalValue != null ? totalValue : 0.0);
            response.put("warehouseId", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching inventory value: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching inventory value: " + e.getMessage()));
        }
    }

    /**
     * Edit inventory addition (same-day only)
     * ✅ UPDATED: Uses RoleConstants.CAN_MODIFY_INVENTORY (includes OWNER)
     */
    @PutMapping("/additions/{additionId}")
    @PreAuthorize(RoleConstants.CAN_MODIFY_INVENTORY)
    public ResponseEntity<?> editAddition(@PathVariable UUID additionId,
                                         @Valid @RequestBody WarehouseInventoryAdditionRequest request,
                                         Authentication authentication) {
        try {
            warehouseInventoryService.editAdditionSameDay(additionId, request, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory addition updated successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error editing addition: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error editing addition: " + e.getMessage()));
        }
    }

    /**
     * Edit inventory withdrawal (same-day only)
     * ✅ UPDATED: Uses RoleConstants.CAN_WITHDRAW_INVENTORY (includes OWNER)
     */
    @PutMapping("/withdrawals/{withdrawalId}")
    @PreAuthorize(RoleConstants.CAN_WITHDRAW_INVENTORY)
    public ResponseEntity<?> editWithdrawal(@PathVariable UUID withdrawalId,
                                           @Valid @RequestBody WarehouseInventoryWithdrawalRequest request,
                                           Authentication authentication) {
        try {
            warehouseInventoryService.editWithdrawalSameDay(withdrawalId, request, authentication);
            
            return ResponseEntity.ok(new ApiResponse(true, "Inventory withdrawal updated successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error editing withdrawal: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error editing withdrawal: " + e.getMessage()));
        }
    }

    /**
     * List inventory additions with filters
     * ✅ UPDATED: Uses RoleConstants.CAN_VIEW_INVENTORY, supports pagination and role-based filtering
     */
    @GetMapping("/warehouse/{warehouseId}/additions")
    @PreAuthorize(RoleConstants.CAN_VIEW_INVENTORY)
    public ResponseEntity<?> listAdditions(
        @PathVariable UUID warehouseId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String transactionType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        try {
            // Get authenticated user from principal
            User currentUser = (User) authentication.getPrincipal();
            
            // Check user role - determine if GM+ (FOUNDER, CEO, GENERAL_MANAGER)
            boolean isGMPlus = isGMPlusRole(currentUser);
            
            // Filter by user if not GM+
            String filterByUsername = isGMPlus ? null : currentUser.getUsername();
            
            logger.info("📥 Fetching additions for warehouse: {} (User: {}, Filter: {})", 
                warehouseId, currentUser.getUsername(), 
                filterByUsername != null ? "Own only" : "All");
            
            Page<WarehouseInventoryAddition> additions = warehouseInventoryService.listAdditions(
                warehouseId, startDate, endDate, transactionType, filterByUsername, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("additions", additions.getContent());
            response.put("currentPage", additions.getNumber());
            response.put("totalPages", additions.getTotalPages());
            response.put("totalItems", additions.getTotalElements());
            response.put("hasMore", additions.hasNext());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching additions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * List inventory withdrawals with filters
     * ✅ UPDATED: Uses RoleConstants.CAN_VIEW_INVENTORY, supports pagination and role-based filtering
     */
    @GetMapping("/warehouse/{warehouseId}/withdrawals")
    @PreAuthorize(RoleConstants.CAN_VIEW_INVENTORY)
    public ResponseEntity<?> listWithdrawals(
        @PathVariable UUID warehouseId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String transactionType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        try {
            // Get authenticated user from principal
            User currentUser = (User) authentication.getPrincipal();
            
            // Check user role - determine if GM+ (FOUNDER, CEO, GENERAL_MANAGER)
            boolean isGMPlus = isGMPlusRole(currentUser);
            
            // Filter by user if not GM+
            String filterByUsername = isGMPlus ? null : currentUser.getUsername();
            
            logger.info("💰 Fetching withdrawals for warehouse: {} (User: {}, Filter: {})", 
                warehouseId, currentUser.getUsername(), 
                filterByUsername != null ? "Own only" : "All");
            
            Page<WarehouseInventoryWithdrawal> withdrawals = warehouseInventoryService.listWithdrawals(
                warehouseId, startDate, endDate, transactionType, filterByUsername, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("withdrawals", withdrawals.getContent());
            response.put("currentPage", withdrawals.getNumber());
            response.put("totalPages", withdrawals.getTotalPages());
            response.put("totalItems", withdrawals.getTotalElements());
            response.put("hasMore", withdrawals.hasNext());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching withdrawals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Check if current user has write permission on warehouse
     * GET /api/warehouse-inventory/warehouse/{warehouseId}/permissions
     */
    @GetMapping("/warehouse/{warehouseId}/permissions")
    public ResponseEntity<?> checkWarehousePermissions(
        @PathVariable UUID warehouseId,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            logger.info("🔐 Checking permissions for user {} on warehouse {}", 
                currentUser.getUsername(), warehouseId);
            
            // Check if warehouse exists
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            
            // Check permissions
            boolean canRead = hasWarehousePermission(currentUser, warehouse, "READ");
            boolean canWrite = hasWarehousePermission(currentUser, warehouse, "WRITE");
            boolean isGMPlus = isGMPlusRole(currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("userId", currentUser.getId());
            response.put("username", currentUser.getUsername());
            response.put("role", currentUser.getRole());
            response.put("permissions", Map.of(
                "canRead", canRead,
                "canWrite", canWrite,
                "canAddInventory", canWrite,
                "canWithdrawInventory", canWrite,
                "isGMPlus", isGMPlus
            ));
            
            logger.info("✅ Permissions: canRead={}, canWrite={}, isGMPlus={}", 
                canRead, canWrite, isGMPlus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error checking permissions: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Check if user has specific permission on warehouse
     */
    private boolean hasWarehousePermission(User user, Warehouse warehouse, String permissionType) {
        // OWNER always has all permissions
        if ("OWNER".equals(user.getRole().name())) {
            logger.info("✅ User is OWNER - has all permissions");
            return true;
        }
        
        // Check if user's company matches warehouse company
        List<CompanyStoreUser> userCompanies = companyStoreUserRepository
            .findByUserAndIsActiveTrue(user);
        
        for (CompanyStoreUser csu : userCompanies) {
            if (csu.getCompany().getId().equals(warehouse.getCompany().getId())) {
                // User is part of the same company
                CompanyRole companyRole = csu.getRole();
                
                // FOUNDER, CEO, GENERAL_MANAGER have all permissions
                if (companyRole == CompanyRole.FOUNDER || 
                    companyRole == CompanyRole.CEO || 
                    companyRole == CompanyRole.GENERAL_MANAGER) {
                    logger.info("✅ User is {} in same company - has all permissions", companyRole);
                    return true;
                }
                
                // STORE_MANAGER has WRITE permission
                if (companyRole == CompanyRole.STORE_MANAGER && "WRITE".equals(permissionType)) {
                    logger.info("✅ User is STORE_MANAGER in same company - has WRITE permission");
                    return true;
                }
                
                // All roles have READ permission
                if ("READ".equals(permissionType)) {
                    logger.info("✅ User is in same company - has READ permission");
                    return true;
                }
            }
        }
        
        logger.warn("⚠️ User does not have {} permission on warehouse", permissionType);
        return false;
    }

    /**
     * Helper method to check if user has GM+ role (FOUNDER, CEO, GENERAL_MANAGER)
     * These roles can view all transactions from all users
     */
    private boolean isGMPlusRole(User user) {
        // Get user's highest company role
        CompanyRole role = warehouseInventoryService.getUserCompanyRole(user);
        return role == CompanyRole.FOUNDER || 
               role == CompanyRole.CEO || 
               role == CompanyRole.GENERAL_MANAGER;
    }
}