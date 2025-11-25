package com.pos.inventsight.controller;

import com.pos.inventsight.dto.GenericApiResponse;
import com.pos.inventsight.dto.SetPriceRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.service.AuditService;
import com.pos.inventsight.service.CompanyAuthorizationService;
import com.pos.inventsight.service.SyncChangeService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Product Pricing", description = "Product price management restricted to CEO, Founder, and General Manager")
public class ProductPricingController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyAuthorizationService authorizationService;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private SyncChangeService syncChangeService;
    
    /**
     * Set original price for a product (CEO, Founder, or General Manager only)
     */
    @PutMapping("/{productId}/price/original")
    @PreAuthorize("hasAnyAuthority('CEO','FOUNDER','GENERAL_MANAGER')")
    @Operation(summary = "Set original price", description = "Set the original price for a product. Restricted to CEO, Founder, and General Manager.")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> setOriginalPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Check if user has access to the product's store
            Store store = product.getStore();
            if (!authorizationService.hasStoreAccess(user, store)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, "Access denied to this product's store", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getOriginalPrice();
            
            // Update the price
            product.setOriginalPrice(request.getAmount());
            productRepository.save(product);
            
            // Log audit event with old and new values
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "original");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", request.getAmount());
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                auditDetails.put("reason", request.getReason());
            }
            auditDetails.put("storeId", store.getId().toString());
            auditDetails.put("storeName", store.getStoreName());
            
            auditService.log(username, user.getUuid(), "PRICE_CHANGE_ORIGINAL", "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", product);
            
            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("productId", productId);
            responseData.put("priceType", "original");
            responseData.put("oldPrice", oldPrice);
            responseData.put("newPrice", request.getAmount());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Original price updated successfully", responseData));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, "Failed to update original price: " + e.getMessage(), null));
        }
    }
    
    /**
     * Set owner-set sell price for a product (CEO, Founder, or General Manager only)
     */
    @PutMapping("/{productId}/price/owner-sell")
    @PreAuthorize("hasAnyAuthority('CEO','FOUNDER','GENERAL_MANAGER')")
    @Operation(summary = "Set owner-sell price", description = "Set the owner-set sell price for a product. Restricted to CEO, Founder, and General Manager.")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> setOwnerSellPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Check if user has access to the product's store
            Store store = product.getStore();
            if (!authorizationService.hasStoreAccess(user, store)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, "Access denied to this product's store", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getOwnerSetSellPrice();
            
            // Update the price
            product.setOwnerSetSellPrice(request.getAmount());
            productRepository.save(product);
            
            // Log audit event with old and new values
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "ownerSetSell");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", request.getAmount());
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                auditDetails.put("reason", request.getReason());
            }
            auditDetails.put("storeId", store.getId().toString());
            auditDetails.put("storeName", store.getStoreName());
            
            auditService.log(username, user.getUuid(), "PRICE_CHANGE_OWNER_SELL", "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", product);
            
            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("productId", productId);
            responseData.put("priceType", "ownerSetSell");
            responseData.put("oldPrice", oldPrice);
            responseData.put("newPrice", request.getAmount());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Owner-sell price updated successfully", responseData));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, "Failed to update owner-sell price: " + e.getMessage(), null));
        }
    }
    
    /**
     * Set retail price for a product (CEO, Founder, or General Manager only)
     */
    @PutMapping("/{productId}/price/retail")
    @PreAuthorize("hasAnyAuthority('CEO','FOUNDER','GENERAL_MANAGER')")
    @Operation(summary = "Set retail price", description = "Set the retail price for a product. Restricted to CEO, Founder, and General Manager.")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> setRetailPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Check if user has access to the product's store
            Store store = product.getStore();
            if (!authorizationService.hasStoreAccess(user, store)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, "Access denied to this product's store", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getRetailPrice();
            
            // Update the price
            product.setRetailPrice(request.getAmount());
            productRepository.save(product);
            
            // Log audit event with old and new values
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("productId", productId.toString());
            auditDetails.put("productName", product.getName());
            auditDetails.put("priceType", "retail");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", request.getAmount());
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                auditDetails.put("reason", request.getReason());
            }
            auditDetails.put("storeId", store.getId().toString());
            auditDetails.put("storeName", store.getStoreName());
            
            auditService.log(username, user.getUuid(), "PRICE_CHANGE_RETAIL", "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", product);
            
            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("productId", productId);
            responseData.put("priceType", "retail");
            responseData.put("oldPrice", oldPrice);
            responseData.put("newPrice", request.getAmount());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Retail price updated successfully", responseData));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, "Failed to update retail price: " + e.getMessage(), null));
        }
    }
}
