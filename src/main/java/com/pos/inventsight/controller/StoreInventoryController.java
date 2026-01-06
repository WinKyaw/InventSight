package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.StoreInventoryAdditionRequest;
import com.pos.inventsight.dto.StoreInventoryAdditionResponse;
import com.pos.inventsight.dto.StoreInventoryBatchAddRequest;
import com.pos.inventsight.dto.StoreInventoryBatchAddResponse;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;
import com.pos.inventsight.model.sql.StoreInventoryAddition;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.security.RoleConstants;
import com.pos.inventsight.service.StoreInventoryService;
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
import java.util.stream.Collectors;

/**
 * REST Controller for store inventory management
 */
@RestController
@RequestMapping("/api/store-inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StoreInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(StoreInventoryController.class);

    @Autowired
    private StoreInventoryService storeInventoryService;

    /**
     * Add inventory to store (restock)
     * POST /api/store-inventory/add
     */
    @PostMapping("/add")
    @PreAuthorize(RoleConstants.CAN_MODIFY_INVENTORY)
    public ResponseEntity<?> addInventory(@Valid @RequestBody StoreInventoryAdditionRequest request,
                                         Authentication authentication) {
        try {
            StoreInventoryAdditionResponse response = storeInventoryService.addInventory(request, authentication);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Inventory added successfully");
            result.put("addition", response);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for adding inventory: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding inventory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error adding inventory: " + e.getMessage()));
        }
    }

    /**
     * Get restock history for a store with pagination
     * GET /api/store-inventory/store/{storeId}/additions
     */
    @GetMapping("/store/{storeId}/additions")
    @PreAuthorize(RoleConstants.CAN_VIEW_INVENTORY)
    public ResponseEntity<?> getAdditions(
        @PathVariable UUID storeId,
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
            
            // Check user role - determine if GM+ (FOUNDER, CEO, GENERAL_MANAGER, ADMIN)
            boolean isGMPlus = isGMPlusRole(currentUser);
            
            // Filter by user if not GM+
            String filterByUsername = isGMPlus ? null : currentUser.getUsername();
            
            logger.info("Fetching additions for store: {} (User: {}, Filter: {})", 
                storeId, currentUser.getUsername(), 
                filterByUsername != null ? "Own only" : "All");
            
            Page<StoreInventoryAddition> additions = storeInventoryService.getAdditions(
                storeId, startDate, endDate, transactionType, filterByUsername, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            
            // Convert entities to DTOs with flattened product info
            List<StoreInventoryAdditionResponse> additionDTOs = additions.getContent()
                .stream()
                .map(StoreInventoryAdditionResponse::new)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storeId", storeId);
            response.put("additions", additionDTOs);
            response.put("currentPage", additions.getNumber());
            response.put("totalPages", additions.getTotalPages());
            response.put("totalItems", additions.getTotalElements());
            response.put("hasMore", additions.hasNext());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching additions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Add multiple items to store inventory (batch restock)
     * POST /api/store-inventory/add-batch
     */
    @PostMapping("/add-batch")
    @PreAuthorize(RoleConstants.CAN_MODIFY_INVENTORY)
    public ResponseEntity<?> addInventoryBatch(
            @Valid @RequestBody StoreInventoryBatchAddRequest request,
            Authentication authentication) {
        try {
            logger.info("📦 Batch restock request for {} items in store: {}", 
                request.getItems().size(), request.getStoreId());
            
            StoreInventoryBatchAddResponse response = 
                storeInventoryService.addInventoryBatch(request, authentication);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getFailedItems() == 0);
            result.put("message", String.format(
                "Batch restock completed: %d successful, %d failed out of %d total",
                response.getSuccessfulItems(),
                response.getFailedItems(),
                response.getTotalItems()
            ));
            result.put("data", response);
            
            // Return 207 Multi-Status if there are partial failures
            if (response.getFailedItems() > 0 && response.getSuccessfulItems() > 0) {
                return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
            }
            
            // Return 200 if all successful
            if (response.getFailedItems() == 0) {
                return ResponseEntity.ok(result);
            }
            
            // Return 400 if all failed
            return ResponseEntity.badRequest().body(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid batch restock request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error in batch restock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error in batch restock: " + e.getMessage()));
        }
    }

    /**
     * Helper method to check if user has GM+ role
     */
    private boolean isGMPlusRole(User user) {
        // Get user's highest company role
        CompanyRole role = storeInventoryService.getUserCompanyRole(user);
        return role == CompanyRole.FOUNDER || 
               role == CompanyRole.CEO || 
               role == CompanyRole.GENERAL_MANAGER;
    }
}
