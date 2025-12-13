package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.GrantPermissionRequest;
import com.pos.inventsight.dto.PermissionResponse;
import com.pos.inventsight.model.sql.OneTimePermission;
import com.pos.inventsight.model.sql.PermissionType;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.service.OneTimePermissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing one-time permissions
 */
@RestController
@RequestMapping("/permissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionController {
    
    @Autowired
    private OneTimePermissionService permissionService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    /**
     * Find user by email or username (supports both login methods)
     * Matches the pattern used in UserService.loadUserByUsername()
     */
    private User findUserByEmailOrUsername(String identifier) {
        return userRepository.findByEmail(identifier)
            .or(() -> userRepository.findByUsername(identifier))
            .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }
    
    /**
     * Grant a one-time permission (GM+ only)
     */
    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> grantPermission(
            @Valid @RequestBody GrantPermissionRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔐 Permission grant request from: " + username);
            
            // Get granting user (supports both email and username)
            User grantedBy = findUserByEmailOrUsername(username);
            
            // Get user to receive permission by UUID (id)
            User grantedTo = userRepository.findById(request.getGrantedToUserId())
                .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            // Get store if specified
            Store store = null;
            if (request.getStoreId() != null) {
                store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new RuntimeException("Store not found"));
            }
            
            // Grant permission
            OneTimePermission permission = permissionService.grantPermission(
                grantedBy, grantedTo, request.getPermissionType(), store);
            
            // Create response
            PermissionResponse response = convertToResponse(permission);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error granting permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to grant permission: " + e.getMessage()));
        }
    }
    
    /**
     * Check if current user has active permission of a specific type
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkPermission(
            @RequestParam PermissionType type,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            // Try email first, then username (same pattern as UserService.loadUserByUsername)
            User user = findUserByEmailOrUsername(username);
            
            boolean hasPermission = permissionService.canPerformAction(user, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasPermission", hasPermission);
            response.put("permissionType", type);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error checking permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to check permission: " + e.getMessage()));
        }
    }
    
    /**
     * Get all active permissions for current user
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActivePermissions(Authentication authentication) {
        try {
            String username = authentication.getName();
            // Try email first, then username (same pattern as UserService.loadUserByUsername)
            User user = findUserByEmailOrUsername(username);
            
            List<OneTimePermission> permissions = permissionService.getActivePermissions(user.getId());
            
            List<PermissionResponse> responses = permissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", responses);
            response.put("count", responses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting active permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to get permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Consume a permission by ID
     */
    @PostMapping("/{id}/consume")
    public ResponseEntity<?> consumePermission(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            // Try email first, then username (same pattern as UserService.loadUserByUsername)
            User user = findUserByEmailOrUsername(username);
            
            System.out.println("🔓 Consuming permission " + id + " for user: " + username);
            
            permissionService.consumePermission(id);
            
            return ResponseEntity.ok(new ApiResponse(true, "Permission consumed successfully"));
            
        } catch (Exception e) {
            System.out.println("❌ Error consuming permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to consume permission: " + e.getMessage()));
        }
    }
    
    /**
     * Get all permissions granted to current user
     */
    @GetMapping("/granted-to-me")
    public ResponseEntity<?> getPermissionsGrantedToMe(Authentication authentication) {
        try {
            String username = authentication.getName();
            // Try email first, then username (same pattern as UserService.loadUserByUsername)
            User user = findUserByEmailOrUsername(username);
            
            List<OneTimePermission> permissions = permissionService.getPermissionsGrantedToUser(user);
            
            List<PermissionResponse> responses = permissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", responses);
            response.put("count", responses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to get permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Get all permissions granted by current user (GM+ only)
     */
    @GetMapping("/granted-by-me")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'CO_OWNER', 'ADMIN')")
    public ResponseEntity<?> getPermissionsGrantedByMe(Authentication authentication) {
        try {
            String username = authentication.getName();
            // Try email first, then username (same pattern as UserService.loadUserByUsername)
            User user = findUserByEmailOrUsername(username);
            
            List<OneTimePermission> permissions = permissionService.getPermissionsGrantedByUser(user);
            
            List<PermissionResponse> responses = permissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("permissions", responses);
            response.put("count", responses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Failed to get permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Convert OneTimePermission to PermissionResponse
     */
    private PermissionResponse convertToResponse(OneTimePermission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setGrantedToUserId(permission.getGrantedToUser().getId());
        response.setGrantedToUsername(permission.getGrantedToUser().getUsername());
        response.setGrantedByUserId(permission.getGrantedByUser().getId());
        response.setGrantedByUsername(permission.getGrantedByUser().getUsername());
        response.setPermissionType(permission.getPermissionType());
        response.setGrantedAt(permission.getGrantedAt());
        response.setExpiresAt(permission.getExpiresAt());
        response.setUsedAt(permission.getUsedAt());
        response.setIsUsed(permission.getIsUsed());
        response.setIsExpired(permission.getIsExpired());
        response.setIsValid(permission.isValid());
        if (permission.getStore() != null) {
            response.setStoreId(permission.getStore().getId());
        }
        return response;
    }
}
