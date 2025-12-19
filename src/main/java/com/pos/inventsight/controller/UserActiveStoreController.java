package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.UserActiveStoreService;
import com.pos.inventsight.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/active-store")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserActiveStoreController {
    
    private final UserActiveStoreService userActiveStoreService;
    private final UserService userService;
    
    public UserActiveStoreController(UserActiveStoreService userActiveStoreService, UserService userService) {
        this.userActiveStoreService = userActiveStoreService;
        this.userService = userService;
    }
    
    /**
     * GET /api/user/active-store - Get current active store
     */
    @GetMapping
    public ResponseEntity<?> getActiveStore(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            Store activeStore = userActiveStoreService.getUserActiveStoreOrThrow(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storeId", activeStore.getId());
            response.put("storeName", activeStore.getStoreName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    /**
     * POST /api/user/active-store - Switch active store (GM+ only)
     */
    @PostMapping
    public ResponseEntity<?> setActiveStore(@RequestParam UUID storeId, 
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            userActiveStoreService.setUserActiveStore(user, storeId);
            
            Store newActiveStore = userActiveStoreService.getUserActiveStoreOrThrow(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Active store changed to: " + newActiveStore.getStoreName());
            response.put("storeId", newActiveStore.getId());
            response.put("storeName", newActiveStore.getStoreName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
}
