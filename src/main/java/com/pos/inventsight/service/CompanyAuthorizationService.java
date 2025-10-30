package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for company-based role authorization and access control
 */
@Service
public class CompanyAuthorizationService {
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private CompanyStoreUserRoleRepository companyStoreUserRoleRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Check if user has access to a company
     */
    public boolean hasCompanyAccess(User user, Company company) {
        return companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(user, company);
    }
    
    /**
     * Check if user has access to a store
     */
    public boolean hasStoreAccess(User user, Store store) {
        if (store == null || store.getCompany() == null) {
            return false;
        }
        
        // Check if user has company access (which includes all stores)
        if (hasCompanyAccess(user, store.getCompany())) {
            return true;
        }
        
        // Check if user has specific store access
        return companyStoreUserRepository.existsByUserAndStoreAndIsActiveTrue(user, store);
    }
    
    /**
     * Check if user has warehouse access
     */
    public boolean hasWarehouseAccess(User user, Warehouse warehouse) {
        if (warehouse == null || warehouse.getCompany() == null) {
            return false;
        }
        
        return hasCompanyAccess(user, warehouse.getCompany());
    }
    
    /**
     * Check if user can manage stores (CEO, founder or general manager)
     */
    public boolean canManageStores(User user, Company company) {
        Optional<CompanyRole> highestRole = getUserHighestRoleInCompany(user, company);
        return highestRole.isPresent() && highestRole.get().canManageStores();
    }
    
    /**
     * Check if user can manage warehouses (CEO, founder or general manager)
     */
    public boolean canManageWarehouses(User user, Company company) {
        Optional<CompanyRole> highestRole = getUserHighestRoleInCompany(user, company);
        return highestRole.isPresent() && highestRole.get().canManageWarehouses();
    }
    
    /**
     * Check if user can manage users in company (CEO, founder or general manager)
     */
    public boolean canManageCompanyUsers(User user, Company company) {
        Optional<CompanyRole> highestRole = getUserHighestRoleInCompany(user, company);
        return highestRole.isPresent() && highestRole.get().canManageUsers();
    }
    
    /**
     * Check if user can manage store operations (store manager level and above)
     */
    public boolean canManageStoreOperations(User user, Store store) {
        if (store == null || store.getCompany() == null) {
            return false;
        }
        
        // Check company-level role
        Optional<CompanyRole> companyRole = companyStoreUserRepository.findUserRoleInCompany(user, store.getCompany());
        if (companyRole.isPresent() && companyRole.get().isManagerLevel()) {
            return true;
        }
        
        // Check store-specific role
        Optional<CompanyRole> storeRole = companyStoreUserRepository.findUserRoleInStore(user, store);
        return storeRole.isPresent() && storeRole.get().isManagerLevel();
    }
    
    /**
     * Check if user has founder privileges in any company
     */
    public boolean isFounder(User user) {
        return companyStoreUserRepository.isFounder(user);
    }
    
    /**
     * Check if user is founder of specific company
     */
    public boolean isFounderOfCompany(User user, Company company) {
        Optional<CompanyRole> role = companyStoreUserRepository.findUserRoleInCompany(user, company);
        return role.isPresent() && role.get() == CompanyRole.FOUNDER;
    }
    
    /**
     * Check if user can approve discounts based on store and role
     */
    public boolean canApproveDiscounts(User user, Store store) {
        return canManageStoreOperations(user, store);
    }
    
    /**
     * Check if user can set owner pricing (founder level only)
     */
    public boolean canSetOwnerPricing(User user, Store store) {
        if (store == null || store.getCompany() == null) {
            return false;
        }
        
        return isFounderOfCompany(user, store.getCompany());
    }
    
    /**
     * Get user's highest role in company
     * Supports many-to-many roles with legacy fallback
     */
    public Optional<CompanyRole> getUserHighestRoleInCompany(User user, Company company) {
        // First try to get roles from new many-to-many mapping
        Optional<CompanyStoreUser> membership = companyStoreUserRepository
            .findByUserAndCompanyAndStoreIsNullAndIsActiveTrue(user, company);
        
        if (membership.isPresent()) {
            List<CompanyRole> roles = companyStoreUserRoleRepository
                .findRolesByCompanyStoreUser(membership.get());
            
            if (!roles.isEmpty()) {
                // Return highest privilege role
                return Optional.of(getHighestRole(roles));
            }
        }
        
        // Fallback to legacy role column
        return companyStoreUserRepository.findUserRoleInCompany(user, company);
    }
    
    /**
     * Get all active roles for a user in a company
     */
    public List<CompanyRole> getUserRolesInCompany(User user, Company company) {
        Optional<CompanyStoreUser> membership = companyStoreUserRepository
            .findByUserAndCompanyAndStoreIsNullAndIsActiveTrue(user, company);
        
        if (membership.isPresent()) {
            List<CompanyRole> roles = companyStoreUserRoleRepository
                .findRolesByCompanyStoreUser(membership.get());
            
            if (!roles.isEmpty()) {
                return roles;
            }
        }
        
        // Fallback to legacy role
        Optional<CompanyRole> legacyRole = companyStoreUserRepository.findUserRoleInCompany(user, company);
        return legacyRole.map(List::of).orElse(List.of());
    }
    
    /**
     * Determine highest privilege role from a list of roles
     */
    private CompanyRole getHighestRole(List<CompanyRole> roles) {
        // Priority order: CEO > FOUNDER > GENERAL_MANAGER > STORE_MANAGER > EMPLOYEE
        if (roles.contains(CompanyRole.CEO)) return CompanyRole.CEO;
        if (roles.contains(CompanyRole.FOUNDER)) return CompanyRole.FOUNDER;
        if (roles.contains(CompanyRole.GENERAL_MANAGER)) return CompanyRole.GENERAL_MANAGER;
        if (roles.contains(CompanyRole.STORE_MANAGER)) return CompanyRole.STORE_MANAGER;
        return CompanyRole.EMPLOYEE;
    }
    
    /**
     * Get user's role in specific store
     */
    public Optional<CompanyRole> getUserRoleInStore(User user, Store store) {
        return companyStoreUserRepository.findUserRoleInStore(user, store);
    }
    
    /**
     * Check access using authentication object
     */
    public boolean hasCompanyAccess(Authentication authentication, Company company) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return hasCompanyAccess(user, company);
    }
    
    /**
     * Check store access using authentication object
     */
    public boolean hasStoreAccess(Authentication authentication, Store store) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return hasStoreAccess(user, store);
    }
    
    /**
     * Check warehouse access using authentication object
     */
    public boolean hasWarehouseAccess(Authentication authentication, Warehouse warehouse) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return hasWarehouseAccess(user, warehouse);
    }
}