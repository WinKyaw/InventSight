package com.pos.inventsight.service;

import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.UnauthorizedException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.SupplyManagementPermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SupplyManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplyManagementService.class);
    
    @Autowired
    private SupplyManagementPermissionRepository permissionRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Check if user has GM+ role (FOUNDER, CEO, GENERAL_MANAGER, or OWNER)
     */
    public boolean isGMPlusUser(User user, Company company) {
        // Check UserRole enum first
        UserRole role = user.getRole();
        if (role == UserRole.OWNER || role == UserRole.FOUNDER) {
            return true;
        }
        
        // Check CompanyRole for company-specific roles
        List<CompanyStoreUser> companyUsers = companyStoreUserRepository
            .findByUserAndCompanyAndIsActiveTrue(user, company);
        
        for (CompanyStoreUser companyUser : companyUsers) {
            CompanyRole companyRole = companyUser.getRole();
            if (companyRole == CompanyRole.FOUNDER || 
                companyRole == CompanyRole.CEO || 
                companyRole == CompanyRole.GENERAL_MANAGER) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if user has supply management permission
     */
    public boolean hasSupplyManagementPermission(User user, Company company) {
        Optional<SupplyManagementPermission> permission = permissionRepository
            .findByUserAndCompanyAndIsActiveTrueAndRevokedAtIsNull(user, company);
        
        if (permission.isPresent()) {
            return permission.get().isValid();
        }
        
        return false;
    }
    
    /**
     * Check if user can manage predefined items (GM+ or has permission)
     */
    public boolean canManagePredefinedItems(User user, Company company) {
        logger.debug("Checking predefined items management permission for user {} in company {}", 
                    user.getUsername(), company.getId());
        
        boolean isGM = isGMPlusUser(user, company);
        boolean hasPermission = hasSupplyManagementPermission(user, company);
        
        logger.debug("User {} - GM+ access: {}, Supply permission: {}", 
                    user.getUsername(), isGM, hasPermission);
        
        return isGM || hasPermission;
    }
    
    /**
     * Verify user can manage predefined items, throw exception if not
     */
    public void verifyCanManagePredefinedItems(User user, Company company) {
        if (!canManagePredefinedItems(user, company)) {
            logger.warn("User {} attempted to access predefined items without permission for company {}", 
                       user.getUsername(), company.getId());
            throw new UnauthorizedException(
                "You don't have permission to manage predefined items. " +
                "Contact a GM+ user to grant Supply Management Specialist permission.");
        }
    }
    
    /**
     * Grant supply management permission to a user
     */
    public SupplyManagementPermission grantPermission(
            User targetUser, 
            Company company, 
            User grantedBy,
            Boolean isPermanent,
            LocalDateTime expiresAt,
            String notes) {
        
        // Verify grantor is GM+
        if (!isGMPlusUser(grantedBy, company)) {
            throw new UnauthorizedException("Only GM+ users can grant supply management permissions");
        }
        
        // Check if permission already exists
        Optional<SupplyManagementPermission> existing = permissionRepository
            .findByUserAndCompanyAndIsActiveTrueAndRevokedAtIsNull(targetUser, company);
        
        if (existing.isPresent() && existing.get().isValid()) {
            logger.info("Permission already exists for user {} in company {}", 
                       targetUser.getUsername(), company.getId());
            return existing.get();
        }
        
        // Create new permission
        SupplyManagementPermission permission = new SupplyManagementPermission(targetUser, company, grantedBy);
        permission.setIsPermanent(isPermanent != null ? isPermanent : true);
        permission.setExpiresAt(expiresAt);
        permission.setNotes(notes);
        
        SupplyManagementPermission saved = permissionRepository.save(permission);
        
        logger.info("Supply management permission granted to user {} in company {} by {}", 
                   targetUser.getUsername(), company.getId(), grantedBy.getUsername());
        
        return saved;
    }
    
    /**
     * Revoke supply management permission
     */
    public void revokePermission(UUID permissionId, User revokedBy) {
        SupplyManagementPermission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with ID: " + permissionId));
        
        // Verify revoker is GM+
        if (!isGMPlusUser(revokedBy, permission.getCompany())) {
            throw new UnauthorizedException("Only GM+ users can revoke supply management permissions");
        }
        
        permission.revoke(revokedBy);
        permissionRepository.save(permission);
        
        logger.info("Supply management permission {} revoked by {}", 
                   permissionId, revokedBy.getUsername());
    }
    
    /**
     * Get all permissions for a user
     */
    public List<SupplyManagementPermission> getUserPermissions(User user) {
        return permissionRepository.findByUserAndIsActiveTrueAndRevokedAtIsNull(user);
    }
    
    /**
     * Get all permissions for a company
     */
    public List<SupplyManagementPermission> getCompanyPermissions(Company company) {
        return permissionRepository.findByCompanyAndIsActiveTrueAndRevokedAtIsNullOrderByGrantedAtDesc(company);
    }
    
    /**
     * Get user's permission for a specific company
     */
    public Optional<SupplyManagementPermission> getUserCompanyPermission(User user, Company company) {
        return permissionRepository.findByUserAndCompanyAndIsActiveTrueAndRevokedAtIsNull(user, company);
    }
    
    /**
     * Expire temporary permissions that have passed their expiration date
     */
    public void expireTemporaryPermissions() {
        List<SupplyManagementPermission> expiredPermissions = 
            permissionRepository.findExpiredPermissions(LocalDateTime.now());
        
        for (SupplyManagementPermission permission : expiredPermissions) {
            permission.setIsActive(false);
            logger.info("Expired temporary permission {} for user {} in company {}", 
                       permission.getId(), 
                       permission.getUser().getUsername(), 
                       permission.getCompany().getId());
        }
        
        if (!expiredPermissions.isEmpty()) {
            permissionRepository.saveAll(expiredPermissions);
            logger.info("Expired {} temporary permissions", expiredPermissions.size());
        }
    }
    
    /**
     * Get user and verify company access
     */
    public User getUserAndVerifyCompanyAccess(Authentication authentication, UUID companyId) {
        User user = userService.getUserByUsername(authentication.getName());
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        
        // Verify user has access to this company
        if (!companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(user, company)) {
            throw new ResourceNotFoundException("You don't have access to company with ID: " + companyId);
        }
        
        return user;
    }
}
