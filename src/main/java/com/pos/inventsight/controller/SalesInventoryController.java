package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.InventoryAvailabilityResponse;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.WarehouseInventory;
import com.pos.inventsight.repository.sql.WarehouseInventoryRepository;
import com.pos.inventsight.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for employee sales inventory read operations
 */
@RestController
@RequestMapping("/sales/inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SalesInventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesInventoryController.class);
    
    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepository;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    /**
     * Get inventory for a specific warehouse (employee view)
     * Returns only available quantity and sale price (no cost information)
     */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> getWarehouseInventory(
            @PathVariable UUID warehouseId,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            logger.info("Fetching inventory for warehouse {} by user {}", warehouseId, username);
            
            List<WarehouseInventory> inventoryList = warehouseInventoryRepository.findByWarehouseId(warehouseId);
            
            List<InventoryAvailabilityResponse> response = new ArrayList<>();
            for (WarehouseInventory inventory : inventoryList) {
                Product product = inventory.getProduct();
                
                InventoryAvailabilityResponse item = new InventoryAvailabilityResponse();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setProductSku(product.getSku());
                item.setWarehouseId(warehouseId);
                item.setWarehouseName(inventory.getWarehouse().getName());
                item.setAvailable(inventory.getAvailableQuantity());
                item.setReorderPoint(inventory.getReorderPoint());
                // Use retail price for employees (not cost price)
                item.setPrice(product.getRetailPrice());
                item.setCurrencyCode("USD"); // Default, should come from company settings
                
                response.add(item);
            }
            
            logger.info("Returned {} inventory items for warehouse {}", response.size(), warehouseId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching warehouse inventory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch warehouse inventory: " + e.getMessage()));
        }
    }
    
    /**
     * Get availability across warehouses for a specific product
     * Allows employees to check cross-store sourcing options
     */
    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")
    public ResponseEntity<?> getProductAvailability(
            @RequestParam UUID productId,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "unknown";
            logger.info("Fetching availability for product {} by user {}", productId, username);
            
            List<WarehouseInventory> inventoryList = warehouseInventoryRepository.findByProductId(productId);
            
            if (inventoryList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Product not found in any warehouse"));
            }
            
            List<InventoryAvailabilityResponse> response = new ArrayList<>();
            for (WarehouseInventory inventory : inventoryList) {
                Product product = inventory.getProduct();
                
                // Only include warehouses with available stock
                if (inventory.getAvailableQuantity() > 0) {
                    InventoryAvailabilityResponse item = new InventoryAvailabilityResponse();
                    item.setProductId(product.getId());
                    item.setProductName(product.getName());
                    item.setProductSku(product.getSku());
                    item.setWarehouseId(inventory.getWarehouse().getId());
                    item.setWarehouseName(inventory.getWarehouse().getName());
                    item.setAvailable(inventory.getAvailableQuantity());
                    item.setReorderPoint(inventory.getReorderPoint());
                    // Use retail price for employees (not cost price)
                    item.setPrice(product.getRetailPrice());
                    item.setCurrencyCode("USD"); // Default, should come from company settings
                    
                    response.add(item);
                }
            }
            
            logger.info("Returned availability for product {} across {} warehouses", 
                productId, response.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching product availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch product availability: " + e.getMessage()));
        }
    }
}
