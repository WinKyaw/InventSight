package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.SetPriceRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.AuditService;
import com.pos.inventsight.service.SyncChangeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing product pricing
 * Restricted to FOUNDER and GENERAL_MANAGER roles only
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductPricingController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private SyncChangeService syncChangeService;
    
    /**
     * Update original price of a product
     * Only FOUNDER, CEO and GENERAL_MANAGER can update prices
     */
    @PutMapping("/{productId}/price/original")
    @PreAuthorize("hasAnyRole('FOUNDER', 'CEO', 'GENERAL_MANAGER')")
    public ResponseEntity<?> updateOriginalPrice(
            @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            BigDecimal oldPrice = product.getOriginalPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setOriginalPrice(newPrice);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
            
            // Audit log
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "original");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            
            auditService.log(username, null, "PRICE_UPDATE", "Product", productId.toString(), auditDetails);
            
            // Activity log
            String description = String.format("Updated original price of '%s' from %s to %s%s",
                    product.getName(), oldPrice, newPrice,
                    request.getReason() != null ? " - Reason: " + request.getReason() : "");
            activityLogService.logActivity(null, username, "PRICE_UPDATE", "PRODUCT", description);
            
            // Sync change for offline sync
            Map<String, Object> changeData = new HashMap<>();
            changeData.put("originalPrice", newPrice);
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", changeData);
            
            System.out.println("💰 Price updated - Original price: " + oldPrice + " -> " + newPrice + " by " + username);
            
            return ResponseEntity.ok(new ApiResponse(true, "Original price updated successfully"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to update original price: " + e.getMessage()));
        }
    }
    
    /**
     * Update owner-set sell price of a product
     * Only FOUNDER, CEO and GENERAL_MANAGER can update prices
     */
    @PutMapping("/{productId}/price/owner-sell")
    @PreAuthorize("hasAnyRole('FOUNDER', 'CEO', 'GENERAL_MANAGER')")
    public ResponseEntity<?> updateOwnerSetSellPrice(
            @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            BigDecimal oldPrice = product.getOwnerSetSellPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setOwnerSetSellPrice(newPrice);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
            
            // Audit log
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "ownerSetSell");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            
            auditService.log(username, null, "PRICE_UPDATE", "Product", productId.toString(), auditDetails);
            
            // Activity log
            String description = String.format("Updated owner-set sell price of '%s' from %s to %s%s",
                    product.getName(), oldPrice, newPrice,
                    request.getReason() != null ? " - Reason: " + request.getReason() : "");
            activityLogService.logActivity(null, username, "PRICE_UPDATE", "PRODUCT", description);
            
            // Sync change for offline sync
            Map<String, Object> changeData = new HashMap<>();
            changeData.put("ownerSetSellPrice", newPrice);
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", changeData);
            
            System.out.println("💰 Price updated - Owner-set sell price: " + oldPrice + " -> " + newPrice + " by " + username);
            
            return ResponseEntity.ok(new ApiResponse(true, "Owner-set sell price updated successfully"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to update owner-set sell price: " + e.getMessage()));
        }
    }
    
    /**
     * Update retail price of a product
     * Only FOUNDER, CEO and GENERAL_MANAGER can update prices
     */
    @PutMapping("/{productId}/price/retail")
    @PreAuthorize("hasAnyRole('FOUNDER', 'CEO', 'GENERAL_MANAGER')")
    public ResponseEntity<?> updateRetailPrice(
            @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            BigDecimal oldPrice = product.getRetailPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setRetailPrice(newPrice);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
            
            // Audit log
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "retail");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            
            auditService.log(username, null, "PRICE_UPDATE", "Product", productId.toString(), auditDetails);
            
            // Activity log
            String description = String.format("Updated retail price of '%s' from %s to %s%s",
                    product.getName(), oldPrice, newPrice,
                    request.getReason() != null ? " - Reason: " + request.getReason() : "");
            activityLogService.logActivity(null, username, "PRICE_UPDATE", "PRODUCT", description);
            
            // Sync change for offline sync
            Map<String, Object> changeData = new HashMap<>();
            changeData.put("retailPrice", newPrice);
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", changeData);
            
            System.out.println("💰 Price updated - Retail price: " + oldPrice + " -> " + newPrice + " by " + username);
            
            return ResponseEntity.ok(new ApiResponse(true, "Retail price updated successfully"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to update retail price: " + e.getMessage()));
        }
    }
}
