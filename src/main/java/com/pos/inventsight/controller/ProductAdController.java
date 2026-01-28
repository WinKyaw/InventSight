package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.ProductAd;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.ProductAdService;
import com.pos.inventsight.service.ImageService;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/marketplace/ads")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductAdController {
    
    @Autowired
    private ProductAdService productAdService;
    
    @Autowired
    private ImageService imageService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    /**
     * Helper method to get user's company
     */
    private Company getUserCompany(User user) {
        List<Company> companies = companyStoreUserRepository.findCompaniesByUser(user);
        if (companies.isEmpty()) {
            return null;
        }
        return companies.get(0); // Return first company
    }
    
    /**
     * POST /api/marketplace/ads - Create product ad
     */
    @PostMapping
    public ResponseEntity<?> createProductAd(@Valid @RequestBody ProductAd productAd,
                                            Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            Store currentStore = userService.getCurrentUserStore();
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            if (currentStore == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "No active store found for current user"));
            }
            
            ProductAd created = productAdService.createProductAd(productAd, company, currentStore, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product ad created successfully");
            response.put("ad", created);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create product ad: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/marketplace/ads - List all active ads (cross-company)
     */
    @GetMapping
    public ResponseEntity<?> getAllActiveAds(@RequestParam(required = false) String search) {
        try {
            List<ProductAd> ads;
            
            if (search != null && !search.trim().isEmpty()) {
                ads = productAdService.searchAdsByProductName(search);
            } else {
                ads = productAdService.getAllActiveAds();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ads", ads);
            response.put("count", ads.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch product ads: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/marketplace/ads/my - List my company's ads
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyCompanyAds(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<ProductAd> ads = productAdService.getAdsByCompany(company.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ads", ads);
            response.put("count", ads.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch company ads: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/marketplace/ads/{id} - Get ad details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAdById(@PathVariable UUID id) {
        try {
            ProductAd ad = productAdService.getAdById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ad", ad);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch product ad: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/marketplace/ads/{id} - Update ad
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProductAd(@PathVariable UUID id,
                                            @Valid @RequestBody ProductAd productAd) {
        try {
            ProductAd updated = productAdService.updateProductAd(id, productAd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product ad updated successfully");
            response.put("ad", updated);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to update product ad: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/marketplace/ads/{id} - Deactivate ad
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateProductAd(@PathVariable UUID id) {
        try {
            productAdService.deactivateProductAd(id);
            
            return ResponseEntity.ok(new ApiResponse(true, "Product ad deactivated successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to deactivate product ad: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/marketplace/ads/{id}/image - Upload product image
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadProductImage(@PathVariable UUID id,
                                               @RequestParam("image") MultipartFile file) {
        try {
            // Upload and process image
            String imageUrl = imageService.uploadImage(file);
            
            // Update product ad with image URL
            ProductAd updated = productAdService.updateProductAdImage(id, imageUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("imageUrl", imageUrl);
            response.put("ad", updated);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to upload image: " + e.getMessage()));
        }
    }
}
