package com.pos.inventsight.controller;

import com.pos.inventsight.dto.GenericApiResponse;
import com.pos.inventsight.dto.GrantSupplyPermissionRequest;
import com.pos.inventsight.dto.SupplyPermissionResponse;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.SupplyManagementPermission;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.CompanyService;
import com.pos.inventsight.service.SupplyManagementService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/supply-management")
@Tag(name = "Supply Management", description = "Supply management permissions")
public class SupplyManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplyManagementController.class);
    
    @Autowired
    private SupplyManagementService supplyManagementService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Grant supply management permission to a user (GM+ only)
     */
    @PostMapping("/permissions")
    @Operation(summary = "Grant permission", description = "Grant supply management permission to a user (GM+ only)")
    public ResponseEntity<GenericApiResponse<SupplyPermissionResponse>> grantPermission(
            @Valid @RequestBody GrantSupplyPermissionRequest request,
            Authentication authentication) {
        
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            User targetUser = userService.getUserById(request.getUserId());
            Company company = companyService.getCompany(request.getCompanyId(), authentication);
            
            SupplyManagementPermission permission = supplyManagementService.grantPermission(
                targetUser,
                company,
                currentUser,
                request.getIsPermanent(),
                request.getExpiresAt(),
                request.getNotes()
            );
            
            SupplyPermissionResponse response = new SupplyPermissionResponse(permission);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericApiResponse<>(true, "Permission granted successfully", response));
            
        } catch (Exception e) {
            logger.error("Error granting permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get permissions for a user
     */
    @GetMapping("/permissions/user/{userId}")
    @Operation(summary = "Get user permissions", description = "Get all supply management permissions for a user")
    public ResponseEntity<GenericApiResponse<List<SupplyPermissionResponse>>> getUserPermissions(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Authentication authentication) {
        
        try {
            User user = userService.getUserById(userId);
            List<SupplyManagementPermission> permissions = supplyManagementService.getUserPermissions(user);
            
            List<SupplyPermissionResponse> response = permissions.stream()
                .map(SupplyPermissionResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Permissions retrieved successfully", response));
            
        } catch (Exception e) {
            logger.error("Error getting user permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get permissions for a company
     */
    @GetMapping("/permissions/company/{companyId}")
    @Operation(summary = "Get company permissions", description = "Get all supply management permissions for a company")
    public ResponseEntity<GenericApiResponse<List<SupplyPermissionResponse>>> getCompanyPermissions(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            User currentUser = userService.getUserByUsername(authentication.getName());
            
            // Verify user is GM+ to view company permissions
            if (!supplyManagementService.isGMPlusUser(currentUser, company)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericApiResponse<>(false, "Only GM+ users can view company permissions", null));
            }
            
            List<SupplyManagementPermission> permissions = supplyManagementService.getCompanyPermissions(company);
            
            List<SupplyPermissionResponse> response = permissions.stream()
                .map(SupplyPermissionResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Permissions retrieved successfully", response));
            
        } catch (Exception e) {
            logger.error("Error getting company permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Revoke a supply management permission (GM+ only)
     */
    @DeleteMapping("/permissions/{permissionId}")
    @Operation(summary = "Revoke permission", description = "Revoke a supply management permission (GM+ only)")
    public ResponseEntity<GenericApiResponse<Void>> revokePermission(
            @Parameter(description = "Permission ID") @PathVariable UUID permissionId,
            Authentication authentication) {
        
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            supplyManagementService.revokePermission(permissionId, currentUser);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Permission revoked successfully"));
            
        } catch (Exception e) {
            logger.error("Error revoking permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage()));
        }
    }
    
    /**
     * Check current user's permission for a company
     */
    @GetMapping("/check")
    @Operation(summary = "Check permission", description = "Check if current user has supply management permission for a company")
    public ResponseEntity<GenericApiResponse<Map<String, Object>>> checkPermission(
            @Parameter(description = "Company ID") @RequestParam UUID companyId,
            Authentication authentication) {
        
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            Company company = companyService.getCompany(companyId, authentication);
            
            boolean isGMPlus = supplyManagementService.isGMPlusUser(currentUser, company);
            boolean hasPermission = supplyManagementService.hasSupplyManagementPermission(currentUser, company);
            boolean canManage = supplyManagementService.canManagePredefinedItems(currentUser, company);
            
            Map<String, Object> response = new HashMap<>();
            response.put("isGMPlus", isGMPlus);
            response.put("hasSupplyPermission", hasPermission);
            response.put("canManagePredefinedItems", canManage);
            
            if (hasPermission) {
                supplyManagementService.getUserCompanyPermission(currentUser, company)
                    .ifPresent(permission -> response.put("permission", new SupplyPermissionResponse(permission)));
            }
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Permission check completed", response));
            
        } catch (Exception e) {
            logger.error("Error checking permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
}
