package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.LowStockService;
import com.pos.inventsight.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/low-stock")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LowStockController {
    
    @Autowired
    private LowStockService lowStockService;
    
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
     * GET /api/low-stock - Get all low stock items for store
     */
    @GetMapping
    public ResponseEntity<?> getLowStockItems(@RequestParam(required = false) UUID storeId,
                                              Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            // If storeId not provided, use user's current store
            if (storeId == null) {
                Store currentStore = userService.getCurrentUserStore();
                if (currentStore == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "No active store found for current user"));
                }
                storeId = currentStore.getId();
            }
            
            List<Map<String, Object>> lowStockItems = lowStockService.getLowStockItemsForStore(storeId, company.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", lowStockItems);
            response.put("count", lowStockItems.size());
            response.put("storeId", storeId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch low stock items: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/low-stock/warehouse/{warehouseId} - Get low stock items for warehouse
     */
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<?> getLowStockItemsForWarehouse(@PathVariable UUID warehouseId,
                                                          Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            Company company = getUserCompany(currentUser);
            
            if (company == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User is not associated with any company"));
            }
            
            List<Map<String, Object>> lowStockItems = lowStockService.getLowStockItemsForWarehouse(warehouseId, company.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", lowStockItems);
            response.put("count", lowStockItems.size());
            response.put("warehouseId", warehouseId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch low stock items: " + e.getMessage()));
        }
    }
}
