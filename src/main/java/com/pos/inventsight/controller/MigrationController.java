package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.service.UuidMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/migration")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MigrationController {
    
    @Autowired
    private UuidMigrationService uuidMigrationService;
    
    // POST /admin/migration/uuid - Run UUID migration
    @PostMapping("/uuid")
    public ResponseEntity<?> runUuidMigration(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔧 InventSight - Running UUID migration initiated by: " + username);
            
            uuidMigrationService.runFullUuidMigration();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "UUID migration completed successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            response.put("initiatedBy", username);
            
            System.out.println("✅ InventSight - UUID migration completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error running UUID migration: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Failed to run UUID migration: " + e.getMessage()));
        }
    }
    
    // POST /admin/migration/uuid/users - Assign UUIDs to users only
    @PostMapping("/uuid/users")
    public ResponseEntity<?> assignUuidsToUsers(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("👤 InventSight - Assigning UUIDs to users initiated by: " + username);
            
            int updatedCount = uuidMigrationService.assignUuidsToUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "UUID assignment to users completed");
            response.put("updatedCount", updatedCount);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            response.put("initiatedBy", username);
            
            System.out.println("✅ InventSight - UUID assignment to users completed: " + updatedCount + " users updated");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error assigning UUIDs to users: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Failed to assign UUIDs to users: " + e.getMessage()));
        }
    }
    
    // POST /admin/migration/uuid/products - Assign UUIDs to products only
    @PostMapping("/uuid/products")
    public ResponseEntity<?> assignUuidsToProducts(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📦 InventSight - Assigning UUIDs to products initiated by: " + username);
            
            int updatedCount = uuidMigrationService.assignUuidsToProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "UUID assignment to products completed");
            response.put("updatedCount", updatedCount);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            response.put("initiatedBy", username);
            
            System.out.println("✅ InventSight - UUID assignment to products completed: " + updatedCount + " products updated");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error assigning UUIDs to products: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Failed to assign UUIDs to products: " + e.getMessage()));
        }
    }
    
    // GET /admin/migration/uuid/validate - Validate UUID assignment
    @GetMapping("/uuid/validate")
    public ResponseEntity<?> validateUuids(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 InventSight - Validating UUIDs initiated by: " + username);
            
            boolean usersValid = uuidMigrationService.validateUserUuids();
            boolean productsValid = uuidMigrationService.validateProductUuids();
            boolean allValid = usersValid && productsValid;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", allValid ? "All UUIDs are valid" : "Some UUIDs are missing or invalid");
            response.put("usersValid", usersValid);
            response.put("productsValid", productsValid);
            response.put("allValid", allValid);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            response.put("validatedBy", username);
            
            System.out.println("✅ InventSight - UUID validation completed: Users=" + usersValid + ", Products=" + productsValid);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error validating UUIDs: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Failed to validate UUIDs: " + e.getMessage()));
        }
    }
}