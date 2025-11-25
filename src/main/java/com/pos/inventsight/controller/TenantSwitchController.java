package com.pos.inventsight.controller;

import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

/**
 * Controller for tenant switching functionality
 * Allows users with multiple company memberships to switch context
 */
@RestController
@RequestMapping("/auth/tenant-switch")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TenantSwitchController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantSwitchController.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Switch to a different tenant context
     * POST /auth/tenant-switch
     * Request body: { "tenant_id": "uuid" }
     */
    @PostMapping
    public ResponseEntity<?> switchTenant(@Valid @RequestBody TenantSwitchRequest request,
                                         Authentication authentication) {
        try {
            // Get authenticated user
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
            }
            
            User user = (User) authentication.getPrincipal();
            logger.info("User {} requesting tenant switch to {}", user.getUsername(), request.getTenantId());
            
            // Parse and validate tenant UUID
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(request.getTenantId());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid tenant_id format"));
            }
            
            // Verify tenant exists
            if (!companyRepository.existsById(tenantUuid)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Tenant not found"));
            }
            
            // Verify user membership in target tenant
            List<CompanyStoreUser> memberships = companyStoreUserRepository
                .findByUserAndIsActiveTrue(user);
            
            boolean hasMembership = memberships.stream()
                .anyMatch(m -> m.getCompany().getId().equals(tenantUuid) && m.getIsActive());
            
            if (!hasMembership) {
                logger.warn("User {} attempted to switch to unauthorized tenant {}", 
                           user.getUsername(), tenantUuid);
                auditService.log(
                    user.getUsername(),
                    user.getUuid(),
                    "TENANT_SWITCH_DENIED",
                    "User",
                    user.getId().toString(),
                    Map.of("target_tenant", tenantUuid.toString())
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Access denied: not a member of target tenant"));
            }
            
            // Generate new short-lived token with tenant_id claim
            String token = jwtUtils.generateJwtToken(user, tenantUuid.toString());
            
            // Audit the tenant switch
            auditService.log(
                user.getUsername(),
                user.getUuid(),
                "TENANT_SWITCH_SUCCESS",
                "User",
                user.getId().toString(),
                Map.of("target_tenant", tenantUuid.toString())
            );
            
            logger.info("User {} successfully switched to tenant {}", user.getUsername(), tenantUuid);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant switched successfully");
            response.put("token", token);
            response.put("tenant_id", tenantUuid.toString());
            response.put("token_type", "Bearer");
            response.put("expires_in", jwtUtils.getJwtExpirationMs() / 1000); // in seconds
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during tenant switch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error switching tenant"));
        }
    }
    
    /**
     * Request body for tenant switch
     */
    public static class TenantSwitchRequest {
        @NotBlank(message = "tenant_id is required")
        private String tenant_id;
        
        public String getTenantId() {
            return tenant_id;
        }
        
        public void setTenantId(String tenant_id) {
            this.tenant_id = tenant_id;
        }
    }
}
