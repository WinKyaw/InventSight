package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.WarehouseInventoryAdditionRequest;
import com.pos.inventsight.dto.WarehouseInventoryRequest;
import com.pos.inventsight.dto.WarehouseInventoryResponse;
import com.pos.inventsight.dto.WarehouseInventoryWithdrawalRequest;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.model.sql.WarehouseInventoryAddition;
import com.pos.inventsight.model.sql.WarehouseInventoryWithdrawal;
import com.pos.inventsight.model.sql.WarehousePermission;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.WarehouseRepository;
import com.pos.inventsight.repository.sql.WarehousePermissionRepository;
import com.pos.inventsight.security.RoleConstants;
import com.pos.inventsight.service.EmployeeService;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private EmployeeService employeeService;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private WarehousePermissionRepository warehousePermissionRepository;

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
            
            boolean isGMPlus = isGMPlusRole(currentUser);
            boolean canRead = false;
            boolean canWrite = false;
            
            // GM+ always has full access
            if (isGMPlus) {
                canRead = true;
                canWrite = true;
                logger.info("✅ User is GM+ - full access granted");
            } else {
                // Check explicit warehouse permission
                Optional<WarehousePermission> permission = warehousePermissionRepository
                    .findByWarehouseIdAndUserIdAndIsActive(warehouseId, currentUser.getId(), true);
                
                if (permission.isPresent()) {
                    WarehousePermission perm = permission.get();
                    canRead = true;
                    canWrite = perm.getPermissionType() == WarehousePermission.PermissionType.READ_WRITE;
                    logger.info("✅ Found explicit permission: {}", perm.getPermissionType());
                } else {
                    logger.warn("⚠️ No explicit permission found for user");
                }
            }
            
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
            
            logger.info("✅ Permissions: canRead={}, canWrite={}", canRead, canWrite);
            
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
     * Get list of users with access to warehouse (GM+ only)
     */
    @GetMapping("/warehouse/{warehouseId}/users")
    public ResponseEntity<?> getWarehouseUsers(
        @PathVariable UUID warehouseId,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            // Only GM+ can view warehouse users
            if (!isGMPlusRole(currentUser)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Insufficient permissions. Only GM+ can view warehouse users."
                ));
            }
            
            logger.info("👥 Fetching users with access to warehouse: {}", warehouseId);
            
            // Get warehouse
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            
            // Get all active permissions for this warehouse
            List<WarehousePermission> permissions = warehousePermissionRepository
                .findByWarehouseIdAndIsActive(warehouseId, true);
            
            List<Map<String, Object>> usersList = permissions.stream()
                .map(permission -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", permission.getUser().getId());
                    userInfo.put("username", permission.getUser().getUsername());
                    userInfo.put("email", permission.getUser().getEmail());
                    userInfo.put("permission", permission.getPermissionType().name());
                    userInfo.put("grantedBy", permission.getGrantedBy());
                    userInfo.put("grantedAt", permission.getGrantedAt());
                    return userInfo;
                })
                .toList();
            
            logger.info("✅ Found {} users with access", usersList.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("warehouseName", warehouse.getName());
            response.put("users", usersList);
            response.put("count", usersList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching warehouse users: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Grant warehouse permission to user (GM+ only)
     */
    @PostMapping("/warehouse/{warehouseId}/permissions")
    public ResponseEntity<?> grantWarehousePermission(
        @PathVariable UUID warehouseId,
        @RequestBody Map<String, Object> request,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            // Only GM+ can grant permissions
            if (!isGMPlusRole(currentUser)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Insufficient permissions. Only GM+ can grant warehouse permissions."
                ));
            }
            
            String userId = (String) request.get("userId");
            String permissionTypeStr = (String) request.get("permissionType");
            
            if (userId == null || permissionTypeStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "userId and permissionType are required"
                ));
            }
            
            logger.info("🔐 Granting {} permission to user {} on warehouse {}", 
                permissionTypeStr, userId, warehouseId);
            
            // Get warehouse
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            
            // Get target user
            User targetUser = userService.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Parse permission type
            WarehousePermission.PermissionType permissionType;
            try {
                permissionType = WarehousePermission.PermissionType.valueOf(permissionTypeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid permission type. Must be READ or READ_WRITE"
                ));
            }
            
            // Check if permission already exists
            Optional<WarehousePermission> existingPermission = warehousePermissionRepository
                .findByWarehouseIdAndUserIdAndIsActive(warehouseId, UUID.fromString(userId), true);
            
            WarehousePermission permission;
            
            if (existingPermission.isPresent()) {
                // Update existing permission
                permission = existingPermission.get();
                permission.setPermissionType(permissionType);
                permission.setGrantedBy(currentUser.getUsername());
                permission.setGrantedAt(LocalDateTime.now());
                logger.info("📝 Updating existing permission");
            } else {
                // Create new permission
                permission = new WarehousePermission(warehouse, targetUser, permissionType);
                permission.setGrantedBy(currentUser.getUsername());
                permission.setGrantedAt(LocalDateTime.now());
                permission.setIsActive(true);
                logger.info("➕ Creating new permission");
            }
            
            permission = warehousePermissionRepository.save(permission);
            
            logger.info("✅ Permission granted successfully");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("permissionId", permission.getId());
            response.put("warehouseId", warehouseId);
            response.put("userId", userId);
            response.put("permissionType", permissionType.name());
            response.put("message", "Permission granted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error granting permission: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Revoke warehouse permission (GM+ only)
     */
    @DeleteMapping("/warehouse/{warehouseId}/permissions/{userId}")
    public ResponseEntity<?> revokeWarehousePermission(
        @PathVariable UUID warehouseId,
        @PathVariable UUID userId,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            // Only GM+ can revoke permissions
            if (!isGMPlusRole(currentUser)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Insufficient permissions. Only GM+ can revoke warehouse permissions."
                ));
            }
            
            logger.info("🔐 Revoking permission for user {} on warehouse {}", userId, warehouseId);
            
            // Find active permission
            Optional<WarehousePermission> permission = warehousePermissionRepository
                .findByWarehouseIdAndUserIdAndIsActive(warehouseId, userId, true);
            
            if (permission.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "No active permission found for this user"
                ));
            }
            
            // Revoke permission (soft delete)
            WarehousePermission perm = permission.get();
            perm.setIsActive(false);
            perm.setRevokedBy(currentUser.getUsername());
            perm.setRevokedAt(LocalDateTime.now());
            warehousePermissionRepository.save(perm);
            
            logger.info("✅ Permission revoked successfully");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Permission revoked successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error revoking permission: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get user's warehouse assignments (permissions)
     * GET /api/warehouse-inventory/user/{userId}/warehouses
     * 
     * Returns list of warehouses the user has access to with their permission types
     */
    @GetMapping("/user/{userId}/warehouses")
    public ResponseEntity<?> getUserWarehouseAssignments(
        @PathVariable UUID userId,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            logger.info("🏢 Fetching warehouse assignments for user: {}", userId);
            
            // Check authorization: user can only view their own assignments unless they're GM+
            if (!currentUser.getId().equals(userId) && !isGMPlusRole(currentUser)) {
                logger.warn("⚠️ Unauthorized: User {} tried to view assignments for user {}", 
                    currentUser.getId(), userId);
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "You can only view your own warehouse assignments"
                ));
            }
            
            // ✅ FIXED: Handle missing user gracefully instead of throwing error
            Optional<User> targetUserOpt = userService.findById(userId);
            
            if (targetUserOpt.isEmpty()) {
                logger.warn("⚠️ User not found: {}, returning empty warehouse list", userId);
                
                // Return empty list instead of error - employee may not have user account yet
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userId", userId);
                response.put("username", null);
                response.put("warehouses", Collections.emptyList());
                response.put("count", 0);
                response.put("message", "User account not found - no warehouses assigned");
                
                return ResponseEntity.ok(response);
            }
            
            User targetUser = targetUserOpt.get();
            
            // Get all active warehouse permissions for this user
            List<WarehousePermission> permissions = warehousePermissionRepository
                .findByUserIdAndIsActive(userId, true);
            
            // Map to response format
            List<Map<String, Object>> assignments = permissions.stream()
                .map(permission -> {
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("id", permission.getId());
                    assignment.put("warehouseId", permission.getWarehouse().getId());
                    assignment.put("warehouseName", permission.getWarehouse().getName());
                    assignment.put("warehouseLocation", permission.getWarehouse().getLocation());
                    assignment.put("permissionType", permission.getPermissionType().name());
                    assignment.put("grantedBy", permission.getGrantedBy());
                    assignment.put("grantedAt", permission.getGrantedAt());
                    assignment.put("isActive", permission.getIsActive());
                    
                    // Add warehouse details
                    Warehouse warehouse = permission.getWarehouse();
                    Map<String, Object> warehouseInfo = new HashMap<>();
                    warehouseInfo.put("id", warehouse.getId());
                    warehouseInfo.put("name", warehouse.getName());
                    warehouseInfo.put("location", warehouse.getLocation());
                    warehouseInfo.put("address", warehouse.getAddress());
                    warehouseInfo.put("city", warehouse.getCity());
                    warehouseInfo.put("state", warehouse.getState());
                    warehouseInfo.put("country", warehouse.getCountry());
                    
                    assignment.put("warehouse", warehouseInfo);
                    
                    return assignment;
                })
                .toList();
            
            logger.info("✅ Found {} warehouse assignments for user {}", assignments.size(), targetUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("username", targetUser.getUsername());
            response.put("warehouses", assignments);
            response.put("count", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching user warehouse assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get employee's warehouse assignments (permissions)
     * ✅ FIXED: Now accepts employeeId and looks up user_id from employee record
     * 
     * GET /api/warehouse-inventory/employee/{employeeId}/warehouses
     */
    @GetMapping("/employee/{employeeId}/warehouses")
    public ResponseEntity<?> getEmployeeWarehouseAssignments(
        @PathVariable UUID employeeId,
        Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            logger.info("🏢 Fetching warehouse assignments for employee: {}", employeeId);
            
            // ✅ STEP 1: Get employee record
            Employee employee = employeeService.getEmployeeById(employeeId);
            
            if (employee == null) {
                logger.warn("⚠️ Employee not found: {}", employeeId);
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Employee not found"
                ));
            }
            
            // ✅ STEP 2: Get user_id from employee
            User targetUser = employee.getUser();
            
            if (targetUser == null) {
                logger.warn("⚠️ Employee {} has no associated user account, returning empty warehouse list", 
                    employee.getFirstName() + " " + employee.getLastName());
                
                // Return empty list - employee exists but has no user account yet
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("employeeId", employeeId);
                response.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                response.put("userId", null);
                response.put("username", null);
                response.put("warehouses", Collections.emptyList());
                response.put("count", 0);
                response.put("message", "Employee has no user account - no warehouses assigned");
                
                return ResponseEntity.ok(response);
            }
            
            UUID userId = targetUser.getId();
            
            logger.info("✅ Employee {} has user account: {} ({})", 
                employee.getFirstName() + " " + employee.getLastName(), targetUser.getUsername(), userId);
            
            // Check authorization: user can only view their own assignments unless they're GM+
            if (!currentUser.getId().equals(userId) && !isGMPlusRole(currentUser)) {
                logger.warn("⚠️ Unauthorized: User {} tried to view assignments for employee {}", 
                    currentUser.getId(), employeeId);
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "You can only view your own warehouse assignments"
                ));
            }
            
            // ✅ STEP 3: Get all active warehouse permissions for this user
            List<WarehousePermission> permissions = warehousePermissionRepository
                .findByUserIdAndIsActive(userId, true);
            
            // Map to response format
            List<Map<String, Object>> assignments = permissions.stream()
                .map(permission -> {
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("id", permission.getId());
                    assignment.put("warehouseId", permission.getWarehouse().getId());
                    assignment.put("warehouseName", permission.getWarehouse().getName());
                    assignment.put("warehouseLocation", permission.getWarehouse().getLocation());
                    assignment.put("permissionType", permission.getPermissionType().name());
                    assignment.put("grantedBy", permission.getGrantedBy());
                    assignment.put("grantedAt", permission.getGrantedAt());
                    assignment.put("isActive", permission.getIsActive());
                    
                    // Add warehouse details
                    Warehouse warehouse = permission.getWarehouse();
                    Map<String, Object> warehouseInfo = new HashMap<>();
                    warehouseInfo.put("id", warehouse.getId());
                    warehouseInfo.put("name", warehouse.getName());
                    warehouseInfo.put("location", warehouse.getLocation());
                    warehouseInfo.put("address", warehouse.getAddress());
                    warehouseInfo.put("city", warehouse.getCity());
                    warehouseInfo.put("state", warehouse.getState());
                    warehouseInfo.put("country", warehouse.getCountry());
                    
                    assignment.put("warehouse", warehouseInfo);
                    
                    return assignment;
                })
                .toList();
            
            logger.info("✅ Found {} warehouse assignments for employee {} (user: {})", 
                assignments.size(), employee.getFirstName() + " " + employee.getLastName(), targetUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employeeId", employeeId);
            response.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
            response.put("userId", userId);
            response.put("username", targetUser.getUsername());
            response.put("warehouses", assignments);
            response.put("count", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching employee warehouse assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
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