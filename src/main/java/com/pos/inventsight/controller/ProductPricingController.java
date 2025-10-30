package com.pos.inventsight.controller;

import com.pos.inventsight.dto.GenericApiResponse;
import com.pos.inventsight.dto.ProductResponse;
import com.pos.inventsight.dto.SetPriceRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.ProductRepository;
import com.pos.inventsight.service.AuditService;
import com.pos.inventsight.service.SyncChangeService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Pricing", description = "Product price management for CEO, Founder, and General Manager roles")
@SecurityRequirement(name = "bearerAuth")
public class ProductPricingController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private SyncChangeService syncChangeService;
    
    /**
     * Update original price (cost price from supplier)
     * Restricted to CEO, FOUNDER, and GENERAL_MANAGER roles
     */
    @PutMapping("/{productId}/price/original")
    @PreAuthorize("hasAnyRole('CEO', 'FOUNDER', 'GENERAL_MANAGER')")
    @Operation(summary = "Update original price", 
               description = "Update product's original/cost price. Restricted to CEO, Founder, and General Manager only.")
    public ResponseEntity<GenericApiResponse<ProductResponse>> updateOriginalPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Load product and verify authorization
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Verify user has pricing management role in the product's company
            if (!hasProductPricingAccess(user, product)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, 
                        "You don't have permission to manage pricing for this product", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getOriginalPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setOriginalPrice(newPrice);
            product.setUpdatedBy(username);
            product.setUpdatedAt(LocalDateTime.now());
            product = productRepository.save(product);
            
            // Log audit event
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("priceType", "original");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            auditDetails.put("productName", product.getName());
            auditDetails.put("productSku", product.getSku());
            
            auditService.log(username, null, "UPDATE_ORIGINAL_PRICE", 
                "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", username);
            
            ProductResponse response = new ProductResponse(product);
            return ResponseEntity.ok(new GenericApiResponse<>(true, 
                "Original price updated successfully", response));
                
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, 
                    "Failed to update original price: " + e.getMessage(), null));
        }
    }
    
    /**
     * Update owner sell price (price set by owner for wholesale/bulk)
     * Restricted to CEO, FOUNDER, and GENERAL_MANAGER roles
     */
    @PutMapping("/{productId}/price/owner-sell")
    @PreAuthorize("hasAnyRole('CEO', 'FOUNDER', 'GENERAL_MANAGER')")
    @Operation(summary = "Update owner sell price", 
               description = "Update product's owner-set sell price. Restricted to CEO, Founder, and General Manager only.")
    public ResponseEntity<GenericApiResponse<ProductResponse>> updateOwnerSellPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Load product and verify authorization
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Verify user has pricing management role in the product's company
            if (!hasProductPricingAccess(user, product)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, 
                        "You don't have permission to manage pricing for this product", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getOwnerSetSellPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setOwnerSetSellPrice(newPrice);
            product.setUpdatedBy(username);
            product.setUpdatedAt(LocalDateTime.now());
            product = productRepository.save(product);
            
            // Log audit event
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("priceType", "ownerSetSell");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            auditDetails.put("productName", product.getName());
            auditDetails.put("productSku", product.getSku());
            
            auditService.log(username, null, "UPDATE_OWNER_SELL_PRICE", 
                "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", username);
            
            ProductResponse response = new ProductResponse(product);
            return ResponseEntity.ok(new GenericApiResponse<>(true, 
                "Owner sell price updated successfully", response));
                
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, 
                    "Failed to update owner sell price: " + e.getMessage(), null));
        }
    }
    
    /**
     * Update retail price (customer-facing price)
     * Restricted to CEO, FOUNDER, and GENERAL_MANAGER roles
     */
    @PutMapping("/{productId}/price/retail")
    @PreAuthorize("hasAnyRole('CEO', 'FOUNDER', 'GENERAL_MANAGER')")
    @Operation(summary = "Update retail price", 
               description = "Update product's retail/customer price. Restricted to CEO, Founder, and General Manager only.")
    public ResponseEntity<GenericApiResponse<ProductResponse>> updateRetailPrice(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            // Load product and verify authorization
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            
            // Verify user has pricing management role in the product's company
            if (!hasProductPricingAccess(user, product)) {
                return ResponseEntity.status(403)
                    .body(new GenericApiResponse<>(false, 
                        "You don't have permission to manage pricing for this product", null));
            }
            
            // Store old value for audit
            BigDecimal oldPrice = product.getRetailPrice();
            BigDecimal newPrice = request.getAmount();
            
            // Update price
            product.setRetailPrice(newPrice);
            product.setUpdatedBy(username);
            product.setUpdatedAt(LocalDateTime.now());
            product = productRepository.save(product);
            
            // Log audit event
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("priceType", "retail");
            auditDetails.put("oldPrice", oldPrice);
            auditDetails.put("newPrice", newPrice);
            auditDetails.put("reason", request.getReason());
            auditDetails.put("productName", product.getName());
            auditDetails.put("productSku", product.getSku());
            
            auditService.log(username, null, "UPDATE_RETAIL_PRICE", 
                "Product", productId.toString(), auditDetails);
            
            // Emit sync change for offline sync
            syncChangeService.recordChange("Product", productId.toString(), "UPDATE", username);
            
            ProductResponse response = new ProductResponse(product);
            return ResponseEntity.ok(new GenericApiResponse<>(true, 
                "Retail price updated successfully", response));
                
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, 
                    "Failed to update retail price: " + e.getMessage(), null));
        }
    }
    
    /**
     * Helper method to check if user has pricing management access for a product
     */
    private boolean hasProductPricingAccess(User user, Product product) {
        if (product.getStore() == null || product.getStore().getCompany() == null) {
            return false;
        }
        
        Company company = product.getStore().getCompany();
        Optional<CompanyRole> role = companyStoreUserRepository.findUserRoleInCompany(user, company);
        
        return role.isPresent() && role.get().canManagePricing();
    }
}
