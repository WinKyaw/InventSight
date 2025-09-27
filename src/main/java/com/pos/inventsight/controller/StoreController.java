package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.StoreRequest;
import com.pos.inventsight.dto.StoreResponse;
import com.pos.inventsight.service.StoreService;
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

@RestController
@RequestMapping("/stores")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StoreController {

    @Autowired
    private StoreService storeService;

    // POST /stores - Create a new store
    @PostMapping
    public ResponseEntity<?> createStore(@Valid @RequestBody StoreRequest storeRequest,
                                       Authentication authentication) {
        try {
            StoreResponse createdStore = storeService.createStore(storeRequest, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Store created successfully");
            response.put("store", createdStore);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error creating store: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error creating store: " + e.getMessage()));
        }
    }

    // GET /stores - Get all stores for authenticated user
    @GetMapping
    public ResponseEntity<?> getUserStores(Authentication authentication) {
        try {
            List<StoreResponse> stores = storeService.getUserStores(authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stores retrieved successfully");
            response.put("stores", stores);
            response.put("count", stores.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error fetching stores: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching stores: " + e.getMessage()));
        }
    }

    // GET /stores/{id} - Get specific store by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getStore(@PathVariable UUID id, Authentication authentication) {
        try {
            StoreResponse store = storeService.getStore(id, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Store retrieved successfully");
            response.put("store", store);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error fetching store: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching store: " + e.getMessage()));
        }
    }

    // POST /stores/{id}/activate - Activate/set store as current tenant context
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateStore(@PathVariable UUID id, Authentication authentication) {
        try {
            StoreResponse activatedStore = storeService.activateStore(id, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Store activated successfully");
            response.put("store", activatedStore);
            response.put("tenantId", authentication.getName()); // The user's UUID is used as tenant ID
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error activating store: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error activating store: " + e.getMessage()));
        }
    }

    // PUT /stores/{id} - Update store information
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStore(@PathVariable UUID id,
                                       @Valid @RequestBody StoreRequest storeRequest,
                                       Authentication authentication) {
        try {
            StoreResponse updatedStore = storeService.updateStore(id, storeRequest, authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Store updated successfully");
            response.put("store", updatedStore);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error updating store: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error updating store: " + e.getMessage()));
        }
    }

    // GET /stores/current - Get current active store based on tenant context
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentStore() {
        try {
            StoreResponse currentStore = storeService.getCurrentStore();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Current store retrieved successfully");
            response.put("store", currentStore);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching current store: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error fetching current store: " + e.getMessage()));
        }
    }
}