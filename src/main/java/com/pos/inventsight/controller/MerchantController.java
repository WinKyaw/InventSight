package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Merchant;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.MerchantService;
import com.pos.inventsight.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MerchantController {
    
    @Autowired
    private MerchantService merchantService;
    
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
     * POST /api/merchants - Create merchant
     */
    @PostMapping
    public ResponseEntity<?> createMerchant(@Valid @RequestBody Merchant merchant, 
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            Merchant created = merchantService.createMerchant(merchant, company, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Merchant created successfully");
            response.put("merchant", created);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to create merchant: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/merchants - List all merchants for company
     */
    @GetMapping
    public ResponseEntity<?> getAllMerchants(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<Merchant> merchants = merchantService.getMerchantsByCompany(company.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("merchants", merchants);
            response.put("count", merchants.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch merchants: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/merchants/{id} - Get merchant details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMerchantById(@PathVariable UUID id, Authentication authentication) {
        try {
            Merchant merchant = merchantService.getMerchantById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("merchant", merchant);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch merchant: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/merchants/{id} - Update merchant
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMerchant(@PathVariable UUID id, 
                                           @Valid @RequestBody Merchant merchant,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            Merchant updated = merchantService.updateMerchant(id, merchant, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Merchant updated successfully");
            response.put("merchant", updated);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to update merchant: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/merchants/{id} - Deactivate merchant
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateMerchant(@PathVariable UUID id, Authentication authentication) {
        try {
            String username = authentication.getName();
            merchantService.deactivateMerchant(id, username);
            
            return ResponseEntity.ok(new ApiResponse(true, "Merchant deactivated successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to deactivate merchant: " + e.getMessage()));
        }
    }
}
