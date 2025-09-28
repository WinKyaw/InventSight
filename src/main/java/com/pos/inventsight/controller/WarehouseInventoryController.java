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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for warehouse inventory management
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
     */
    @PostMapping
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
     */
    @PostMapping("/add")
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
     */
    @PostMapping("/withdraw")
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
     */
    @PostMapping("/reserve")
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
     */
    @PostMapping("/release")
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
     */
    @GetMapping("/warehouse/{warehouseId}")
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
}