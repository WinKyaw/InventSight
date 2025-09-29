package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.WarehouseRequest;
import com.pos.inventsight.dto.WarehouseResponse;
import com.pos.inventsight.model.sql.Warehouse;
import com.pos.inventsight.service.WarehouseService;
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
 * REST Controller for warehouse management
 */
@RestController
@RequestMapping("/api/warehouses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    /**
     * Create a new warehouse
     * POST /api/warehouses
     */
    @PostMapping
    public ResponseEntity<?> createWarehouse(@Valid @RequestBody WarehouseRequest request,
                                           Authentication authentication) {
        try {
            WarehouseResponse warehouse = warehouseService.createWarehouse(request, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Warehouse created successfully");
            response.put("warehouse", warehouse);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error creating warehouse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error creating warehouse: " + e.getMessage()));
        }
    }

    /**
     * Get all warehouses
     * GET /api/warehouses
     */
    @GetMapping
    public ResponseEntity<?> getAllWarehouses() {
        try {
            List<WarehouseResponse> warehouses = warehouseService.getAllActiveWarehouses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouses", warehouses);
            response.put("count", warehouses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouses: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching warehouses: " + e.getMessage()));
        }
    }

    /**
     * Get warehouse by ID
     * GET /api/warehouses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWarehouseById(@PathVariable UUID id) {
        try {
            WarehouseResponse warehouse = warehouseService.getWarehouseById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouse", warehouse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching warehouse: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Warehouse not found: " + e.getMessage()));
        }
    }

    /**
     * Update warehouse
     * PUT /api/warehouses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable UUID id,
                                           @Valid @RequestBody WarehouseRequest request,
                                           Authentication authentication) {
        try {
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
     * Delete warehouse (deactivate)
     * DELETE /api/warehouses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable UUID id,
                                           Authentication authentication) {
        try {
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
}