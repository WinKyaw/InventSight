package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for handling UUID migration and assignment for existing data
 */
@Service
@Transactional
public class UuidMigrationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    /**
     * Assign UUIDs to all users that don't have them
     * This method is idempotent - it won't overwrite existing UUIDs
     */
    public int assignUuidsToUsers() {
        List<User> users = userRepository.findAll();
        int updatedCount = 0;
        
        for (User user : users) {
            if (user.getUuid() == null) {
                UUID newUuid = UUID.randomUUID();
                user.setUuid(newUuid);
                user.setTenantId(newUuid); // Use UUID as tenant ID
                
                userRepository.save(user);
                updatedCount++;
                
                System.out.println("✅ Assigned UUID to user: " + user.getUsername() + " -> " + newUuid);
                
                // Log the activity
                activityLogService.logActivity(
                    user.getId().toString(),
                    "SYSTEM_MIGRATION",
                    "UUID_ASSIGNED",
                    "USER",
                    "UUID assigned to user: " + user.getUsername()
                );
            }
        }
        
        System.out.println("📊 UUID assignment completed. Updated " + updatedCount + " users.");
        return updatedCount;
    }
    
    /**
     * Assign UUIDs to all products that don't have them
     * This method is idempotent - it won't overwrite existing UUIDs
     */
    public int assignUuidsToProducts() {
        List<Product> products = productRepository.findAll();
        int updatedCount = 0;
        
        for (Product product : products) {
            if (product.getUuid() == null || product.getUuid().trim().isEmpty()) {
                String newUuid = UUID.randomUUID().toString();
                product.setUuid(newUuid);
                
                productRepository.save(product);
                updatedCount++;
                
                System.out.println("✅ Assigned UUID to product: " + product.getName() + " -> " + newUuid);
                
                // Log the activity
                activityLogService.logActivity(
                    null,
                    "SYSTEM_MIGRATION",
                    "UUID_ASSIGNED",
                    "PRODUCT",
                    "UUID assigned to product: " + product.getName()
                );
            }
        }
        
        System.out.println("📊 UUID assignment completed. Updated " + updatedCount + " products.");
        return updatedCount;
    }
    
    /**
     * Run full UUID migration for both users and products
     */
    public void runFullUuidMigration() {
        System.out.println("🔧 Starting UUID migration...");
        
        int userCount = assignUuidsToUsers();
        int productCount = assignUuidsToProducts();
        
        System.out.println("✅ UUID migration completed successfully!");
        System.out.println("📊 Summary: " + userCount + " users, " + productCount + " products updated");
        
        // Log the migration completion
        activityLogService.logActivity(
            null,
            "SYSTEM_MIGRATION",
            "MIGRATION_COMPLETED",
            "SYSTEM",
            "UUID migration completed. Updated " + userCount + " users and " + productCount + " products"
        );
    }
    
    /**
     * Validate that all users have valid UUIDs and tenant IDs
     */
    public boolean validateUserUuids() {
        List<User> users = userRepository.findAll();
        boolean allValid = true;
        
        for (User user : users) {
            if (user.getUuid() == null) {
                System.err.println("❌ User missing UUID: " + user.getUsername());
                allValid = false;
            }
            if (user.getTenantId() == null) {
                System.err.println("❌ User missing tenant ID: " + user.getUsername());
                allValid = false;
            }
        }
        
        if (allValid) {
            System.out.println("✅ All users have valid UUIDs and tenant IDs");
        }
        
        return allValid;
    }
    
    /**
     * Validate that all products have valid UUIDs
     */
    public boolean validateProductUuids() {
        List<Product> products = productRepository.findAll();
        boolean allValid = true;
        
        for (Product product : products) {
            if (product.getUuid() == null || product.getUuid().trim().isEmpty()) {
                System.err.println("❌ Product missing UUID: " + product.getName());
                allValid = false;
            }
        }
        
        if (allValid) {
            System.out.println("✅ All products have valid UUIDs");
        }
        
        return allValid;
    }
}