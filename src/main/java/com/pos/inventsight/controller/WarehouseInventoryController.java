package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.WarehouseInventoryAdditionRequest;
import com.pos.inventsight.dto.WarehouseInventoryRequest;
import com.pos.inventsight.dto.WarehouseInventoryResponse;
import com.pos.inventsight.dto.WarehouseInventoryWithdrawalRequest;
import com.pos.inventsight.service.WarehouseInventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@RestController
@RequestMapping("/api/warehouse-inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WarehouseInventoryController {

    @Autowired
    private WarehouseInventoryService warehouseInventoryService;

    /**
     * Create or update warehouse inventory
     * POST /api/warehouse-inventory
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER only
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER')")
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
     * POST /api/warehouse-inventory/add
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER', 'EMPLOYEE')")
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
     * POST /api/warehouse-inventory/withdraw
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE
     */
    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER', 'EMPLOYEE')")
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
     * POST /api/warehouse-inventory/reserve
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER')")
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
     * POST /api/warehouse-inventory/release
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER
     */
    @PostMapping("/release")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER')")
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
     * GET /api/warehouse-inventory/warehouse/{warehouseId}
     * RBAC: All authenticated users can view
     */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getWarehouseInventory(@PathVariable UUID warehouseId) {
        try {
            List<WarehouseInventoryResponse> inventory = warehouseInventoryService.getWarehouseInventory(warehouseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inventory", inventory);
            response.put("count", inventory.size());
            response.put("warehouseId", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouse inventory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching inventory: " + e.getMessage()));
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
     * GET /api/warehouse-inventory/warehouse/{warehouseId}/value
     */
    @GetMapping("/warehouse/{warehouseId}/value")
    @PreAuthorize("isAuthenticated()")
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
     * PUT /api/warehouse-inventory/additions/{additionId}
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE (own only)
     */
    @PutMapping("/additions/{additionId}")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER', 'EMPLOYEE')")
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
     * PUT /api/warehouse-inventory/withdrawals/{withdrawalId}
     * RBAC: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE (own only)
     */
    @PutMapping("/withdrawals/{withdrawalId}")
    @PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER', 'EMPLOYEE')")
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
     * GET /api/warehouse-inventory/warehouse/{warehouseId}/additions
     * RBAC: All authenticated users can view
     */
    @GetMapping("/warehouse/{warehouseId}/additions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listAdditions(@PathVariable UUID warehouseId,
                                          @RequestParam(required = false) LocalDate startDate,
                                          @RequestParam(required = false) LocalDate endDate,
                                          @RequestParam(required = false) String transactionType) {
        try {
            List<?> additions = warehouseInventoryService.listAdditions(warehouseId, startDate, endDate, transactionType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("additions", additions);
            response.put("count", additions.size());
            response.put("warehouseId", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error listing additions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error listing additions: " + e.getMessage()));
        }
    }

    /**
     * List inventory withdrawals with filters
     * GET /api/warehouse-inventory/warehouse/{warehouseId}/withdrawals
     * RBAC: All authenticated users can view
     */
    @GetMapping("/warehouse/{warehouseId}/withdrawals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listWithdrawals(@PathVariable UUID warehouseId,
                                            @RequestParam(required = false) LocalDate startDate,
                                            @RequestParam(required = false) LocalDate endDate,
                                            @RequestParam(required = false) String transactionType) {
        try {
            List<?> withdrawals = warehouseInventoryService.listWithdrawals(warehouseId, startDate, endDate, transactionType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("withdrawals", withdrawals);
            response.put("count", withdrawals.size());
            response.put("warehouseId", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error listing withdrawals: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error listing withdrawals: " + e.getMessage()));
        }
    }
}