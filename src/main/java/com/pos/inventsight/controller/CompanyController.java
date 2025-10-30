package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.CompanyService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Company Management", description = "Company-centric multi-tenant operations")
public class CompanyController {
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Create a new company with the user as founder
     */
    @PostMapping
    @Operation(summary = "Create new company", description = "Create a new company with the authenticated user as founder")
    public ResponseEntity<GenericApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CompanyRequest request,
            Authentication authentication) {
        
        try {
            Company company = companyService.createCompany(
                request.getName(), 
                request.getDescription(), 
                request.getEmail(), 
                authentication
            );
            
            CompanyResponse response = new CompanyResponse(company);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericApiResponse<>(true, "Company created successfully", response));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get all companies the user has access to
     */
    @GetMapping
    @Operation(summary = "Get user companies", description = "Get all companies the authenticated user has access to")
    public ResponseEntity<GenericApiResponse<List<CompanyResponse>>> getUserCompanies(Authentication authentication) {
        
        try {
            List<Company> companies = companyService.getUserCompanies(authentication);
            List<CompanyResponse> responses = companies.stream()
                .map(CompanyResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Companies retrieved successfully", responses));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get specific company by ID
     */
    @GetMapping("/{companyId}")
    @Operation(summary = "Get company by ID", description = "Get specific company details by ID")
    public ResponseEntity<GenericApiResponse<CompanyResponse>> getCompany(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            Authentication authentication) {
        
        try {
            Company company = companyService.getCompany(companyId, authentication);
            CompanyResponse response = new CompanyResponse(company);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Company retrieved successfully", response));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update company information
     */
    @PutMapping("/{companyId}")
    @Operation(summary = "Update company", description = "Update company information")
    public ResponseEntity<GenericApiResponse<CompanyResponse>> updateCompany(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Valid @RequestBody CompanyRequest request,
            Authentication authentication) {
        
        try {
            Company company = companyService.updateCompany(
                companyId,
                request.getName(),
                request.getDescription(),
                request.getEmail(),
                authentication
            );
            
            CompanyResponse response = new CompanyResponse(company);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Company updated successfully", response));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Add user to company
     */
    @PostMapping("/{companyId}/users")
    @Operation(summary = "Add user to company", description = "Add a user to company with specified role")
    public ResponseEntity<GenericApiResponse<CompanyUserResponse>> addUserToCompany(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Valid @RequestBody CompanyUserRequest request,
            Authentication authentication) {
        
        try {
            // Find user by username or email
            User userToAdd = findUserByUsernameOrEmail(request.getUsernameOrEmail());
            
            CompanyStoreUser companyStoreUser;
            if (request.getStoreId() != null) {
                // Add to specific store
                companyStoreUser = companyService.addUserToStore(
                    companyId, request.getStoreId(), userToAdd, request.getRole(), authentication
                );
            } else {
                // Add to company level
                companyStoreUser = companyService.addUserToCompany(
                    companyId, userToAdd, request.getRole(), authentication
                );
            }
            
            CompanyUserResponse response = new CompanyUserResponse(companyStoreUser);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericApiResponse<>(true, "User added successfully", response));
                
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get company users
     */
    @GetMapping("/{companyId}/users")
    @Operation(summary = "Get company users", description = "Get all users in a company")
    public ResponseEntity<GenericApiResponse<List<CompanyUserResponse>>> getCompanyUsers(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            Authentication authentication) {
        
        try {
            List<CompanyStoreUser> companyUsers = companyService.getCompanyUsers(companyId, authentication);
            List<CompanyUserResponse> responses = companyUsers.stream()
                .map(CompanyUserResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Users retrieved successfully", responses));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Remove user from company
     */
    @DeleteMapping("/{companyId}/users/{userId}")
    @Operation(summary = "Remove user from company", description = "Remove user from company and all associated stores")
    public ResponseEntity<GenericApiResponse<String>> removeUserFromCompany(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            Authentication authentication) {
        
        try {
            User userToRemove = userService.getUserByUuid(userId);
            companyService.removeUserFromCompany(companyId, userToRemove, authentication);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "User removed from company successfully", null));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get store users
     */
    @GetMapping("/stores/{storeId}/users")
    @Operation(summary = "Get store users", description = "Get all users assigned to a specific store")
    public ResponseEntity<GenericApiResponse<List<CompanyUserResponse>>> getStoreUsers(
            @Parameter(description = "Store ID") @PathVariable UUID storeId,
            Authentication authentication) {
        
        try {
            List<CompanyStoreUser> storeUsers = companyService.getStoreUsers(storeId, authentication);
            List<CompanyUserResponse> responses = storeUsers.stream()
                .map(CompanyUserResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Store users retrieved successfully", responses));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Add role to a membership (many-to-many support)
     */
    @PostMapping("/{companyId}/memberships/{membershipId}/roles")
    @Operation(summary = "Add role to membership", description = "Add an additional role to an existing company membership")
    public ResponseEntity<GenericApiResponse<CompanyStoreUserRoleResponse>> addRoleToMembership(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Parameter(description = "Membership ID") @PathVariable UUID membershipId,
            @Valid @RequestBody AddRoleRequest request,
            Authentication authentication) {
        
        try {
            CompanyStoreUserRole roleMapping = companyService.addRoleToMembership(
                companyId, membershipId, request.getRole(), authentication
            );
            
            CompanyStoreUserRoleResponse response = new CompanyStoreUserRoleResponse(roleMapping);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericApiResponse<>(true, "Role added successfully", response));
                
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Remove role from a membership (many-to-many support)
     */
    @DeleteMapping("/{companyId}/memberships/{membershipId}/roles/{role}")
    @Operation(summary = "Remove role from membership", description = "Remove a specific role from a company membership")
    public ResponseEntity<GenericApiResponse<String>> removeRoleFromMembership(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Parameter(description = "Membership ID") @PathVariable UUID membershipId,
            @Parameter(description = "Role to remove") @PathVariable CompanyRole role,
            Authentication authentication) {
        
        try {
            companyService.removeRoleFromMembership(companyId, membershipId, role, authentication);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Role removed successfully", null));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get all roles for a membership
     */
    @GetMapping("/{companyId}/memberships/{membershipId}/roles")
    @Operation(summary = "Get membership roles", description = "Get all roles assigned to a membership")
    public ResponseEntity<GenericApiResponse<List<CompanyStoreUserRoleResponse>>> getMembershipRoles(
            @Parameter(description = "Company ID") @PathVariable UUID companyId,
            @Parameter(description = "Membership ID") @PathVariable UUID membershipId,
            Authentication authentication) {
        
        try {
            List<CompanyStoreUserRole> roleMappings = companyService.getMembershipRoles(membershipId, authentication);
            List<CompanyStoreUserRoleResponse> responses = roleMappings.stream()
                .map(CompanyStoreUserRoleResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Roles retrieved successfully", responses));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Helper method to find user by username or email
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        try {
            // Try finding by username first
            return userService.getUserByUsername(usernameOrEmail);
        } catch (ResourceNotFoundException e) {
            // If not found by username, try by email
            return userService.getUserByEmail(usernameOrEmail);
        }
    }
}