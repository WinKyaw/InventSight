package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.PreexistingItemRequest;
import com.pos.inventsight.model.sql.PreexistingItem;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.service.PreexistingItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing preexisting catalog items
 */
@RestController
@RequestMapping("/preexisting-items")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PreexistingItemController {
    
    @Autowired
    private PreexistingItemService itemService;
    
    @Autowired
    private StoreRepository storeRepository;
    
    /**
     * Get all items for a store
     */
    @GetMapping
    public ResponseEntity<?> getAllItems(
            @RequestParam UUID storeId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📦 Fetching preexisting items for store: " + storeId + ", user: " + username);
            
            List<PreexistingItem> items = itemService.getItemsByStore(storeId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("count", items.size());
            response.put("storeId", storeId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error fetching items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch items: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new item (GM+ only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> createItem(
            @Valid @RequestBody PreexistingItemRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("➕ Creating preexisting item for user: " + username);
            
            // Get store
            Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));
            
            // Create item
            PreexistingItem item = new PreexistingItem();
            item.setStore(store);
            item.setItemName(request.getItemName());
            item.setCategory(request.getCategory());
            item.setDefaultPrice(request.getDefaultPrice());
            item.setDescription(request.getDescription());
            item.setSku(request.getSku());
            
            PreexistingItem saved = itemService.createItem(item, username);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (Exception e) {
            System.out.println("❌ Error creating item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to create item: " + e.getMessage()));
        }
    }
    
    /**
     * Update an item (GM+ only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody PreexistingItemRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("✏️ Updating preexisting item: " + id + ", user: " + username);
            
            // Get store
            Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));
            
            // Update item
            PreexistingItem updatedItem = new PreexistingItem();
            updatedItem.setStore(store);
            updatedItem.setItemName(request.getItemName());
            updatedItem.setCategory(request.getCategory());
            updatedItem.setDefaultPrice(request.getDefaultPrice());
            updatedItem.setDescription(request.getDescription());
            updatedItem.setSku(request.getSku());
            
            PreexistingItem saved = itemService.updateItem(id, updatedItem);
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            System.out.println("❌ Error updating item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to update item: " + e.getMessage()));
        }
    }
    
    /**
     * Soft delete an item (GM+ only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteItem(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ Deleting preexisting item: " + id + ", user: " + username);
            
            itemService.deleteItem(id);
            
            return ResponseEntity.ok(new ApiResponse(true, "Item deleted successfully"));
            
        } catch (Exception e) {
            System.out.println("❌ Error deleting item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to delete item: " + e.getMessage()));
        }
    }
    
    /**
     * Export items as JSON (GM+ only)
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> exportItems(
            @RequestParam UUID storeId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📤 Exporting items for store: " + storeId + ", user: " + username);
            
            String json = itemService.exportItemsToJson(storeId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", 
                "preexisting-items-" + storeId + ".json");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(json);
            
        } catch (Exception e) {
            System.out.println("❌ Error exporting items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to export items: " + e.getMessage()));
        }
    }
    
    /**
     * Import items from JSON file (GM+ only)
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> importItems(
            @RequestParam UUID targetStoreId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📥 Importing items to store: " + targetStoreId + ", user: " + username);
            
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "File is empty"));
            }
            
            if (!file.getContentType().equals("application/json")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "File must be JSON format"));
            }
            
            int importedCount = itemService.importItemsFromJson(targetStoreId, file, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Items imported successfully");
            response.put("importedCount", importedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error importing items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to import items: " + e.getMessage()));
        }
    }
    
    /**
     * Search items by name
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            @RequestParam UUID storeId,
            @RequestParam String query,
            Authentication authentication) {
        try {
            List<PreexistingItem> items = itemService.searchItems(storeId, query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("count", items.size());
            response.put("query", query);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error searching items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to search items: " + e.getMessage()));
        }
    }
}
