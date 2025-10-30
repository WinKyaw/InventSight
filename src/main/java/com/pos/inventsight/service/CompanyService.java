package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.PlanLimitExceededException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRoleRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
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
public class CompanyService {
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private CompanyStoreUserRoleRepository companyStoreUserRoleRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Create a new company with the user as founder
     */
    public Company createCompany(String name, String description, String email, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        // Check subscription limits
        checkSubscriptionLimit(user);
        
        // Check if company name already exists
        if (companyRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Company with name '" + name + "' already exists");
        }
        
        // Check if email is already used
        if (email != null && companyRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Company with email '" + email + "' already exists");
        }
        
        // Create company
        Company company = new Company(name, description, email, username);
        Company savedCompany = companyRepository.save(company);
        
        // Create company-user relationship with founder role
        CompanyStoreUser companyStoreUser = new CompanyStoreUser(savedCompany, user, CompanyRole.FOUNDER, username);
        CompanyStoreUser savedMembership = companyStoreUserRepository.save(companyStoreUser);
        
        // Create role mapping entry for many-to-many support
        CompanyStoreUserRole roleMapping = new CompanyStoreUserRole(savedMembership, CompanyRole.FOUNDER, username);
        companyStoreUserRoleRepository.save(roleMapping);
        
        System.out.println("🏢 Company created: " + savedCompany.getName() + " (ID: " + savedCompany.getId() + ") with founder: " + username);
        
        return savedCompany;
    }
    
    /**
     * Get all companies that a user has access to
     */
    public List<Company> getUserCompanies(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return companyStoreUserRepository.findCompaniesByUser(user);
    }
    
    /**
     * Get a specific company by ID if user has access
     */
    public Company getCompany(UUID companyId, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        
        // Check if user has access to this company
        if (!companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(user, company)) {
            throw new ResourceNotFoundException("You don't have access to company with ID: " + companyId);
        }
        
        return company;
    }
    
    /**
     * Update company information
     */
    public Company updateCompany(UUID companyId, String name, String description, String email, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        
        // Check if user has permission to update (founder or general manager)
        Optional<CompanyRole> userRole = companyStoreUserRepository.findUserRoleInCompany(user, company);
        if (userRole.isEmpty() || (!userRole.get().canManageUsers())) {
            throw new RuntimeException("You don't have permission to update this company");
        }
        
        // Check for name conflicts if name is being changed
        if (name != null && !name.equals(company.getName()) && companyRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Company with name '" + name + "' already exists");
        }
        
        // Check for email conflicts if email is being changed
        if (email != null && !email.equals(company.getEmail()) && companyRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Company with email '" + email + "' already exists");
        }
        
        // Update company details
        if (name != null) company.setName(name);
        if (description != null) company.setDescription(description);
        if (email != null) company.setEmail(email);
        company.setUpdatedBy(username);
        company.setUpdatedAt(LocalDateTime.now());
        
        return companyRepository.save(company);
    }
    
    /**
     * Add user to company with specified role
     */
    public CompanyStoreUser addUserToCompany(UUID companyId, User userToAdd, CompanyRole role, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        
        // Check if current user has permission to add users
        Optional<CompanyRole> currentUserRole = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
        if (currentUserRole.isEmpty() || (!currentUserRole.get().canManageUsers())) {
            throw new RuntimeException("You don't have permission to add users to this company");
        }
        
        // Check if user is already in the company
        if (companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(userToAdd, company)) {
            throw new DuplicateResourceException("User is already a member of this company");
        }
        
        // Create company-user relationship
        CompanyStoreUser companyStoreUser = new CompanyStoreUser(company, userToAdd, role, username);
        return companyStoreUserRepository.save(companyStoreUser);
    }
    
    /**
     * Add user to specific store within company
     */
    public CompanyStoreUser addUserToStore(UUID companyId, UUID storeId, User userToAdd, CompanyRole role, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));
        
        // Verify store belongs to company
        if (!store.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Store does not belong to the specified company");
        }
        
        // Check if current user has permission
        Optional<CompanyRole> currentUserRole = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
        if (currentUserRole.isEmpty() || (!currentUserRole.get().canManageUsers())) {
            throw new RuntimeException("You don't have permission to add users to this store");
        }
        
        // Check if user-store relationship already exists
        if (companyStoreUserRepository.existsByUserAndStoreAndIsActiveTrue(userToAdd, store)) {
            throw new DuplicateResourceException("User is already assigned to this store");
        }
        
        // Create company-store-user relationship
        CompanyStoreUser companyStoreUser = new CompanyStoreUser(company, store, userToAdd, role, username);
        return companyStoreUserRepository.save(companyStoreUser);
    }
    
    /**
     * Remove user from company
     */
    public void removeUserFromCompany(UUID companyId, User userToRemove, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        
        // Check if current user has permission
        Optional<CompanyRole> currentUserRole = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
        if (currentUserRole.isEmpty() || (!currentUserRole.get().canManageUsers())) {
            throw new RuntimeException("You don't have permission to remove users from this company");
        }
        
        // Find and deactivate all relationships for this user in this company
        List<CompanyStoreUser> userRelationships = companyStoreUserRepository.findByUserAndCompanyAndIsActiveTrue(userToRemove, company);
        for (CompanyStoreUser relationship : userRelationships) {
            relationship.revokeRole(username);
            companyStoreUserRepository.save(relationship);
        }
    }
    
    /**
     * Get user's role in company
     */
    public Optional<CompanyRole> getUserRoleInCompany(User user, Company company) {
        return companyStoreUserRepository.findUserRoleInCompany(user, company);
    }
    
    /**
     * Get user's role in store
     */
    public Optional<CompanyRole> getUserRoleInStore(User user, Store store) {
        return companyStoreUserRepository.findUserRoleInStore(user, store);
    }
    
    /**
     * Check if user has access to company
     */
    public boolean hasCompanyAccess(User user, Company company) {
        return companyStoreUserRepository.existsByUserAndCompanyAndIsActiveTrue(user, company);
    }
    
    /**
     * Check if user has access to store
     */
    public boolean hasStoreAccess(User user, Store store) {
        return companyStoreUserRepository.existsByUserAndStoreAndIsActiveTrue(user, store);
    }
    
    /**
     * Get company users
     */
    public List<CompanyStoreUser> getCompanyUsers(UUID companyId, Authentication authentication) {
        Company company = getCompany(companyId, authentication);
        return companyStoreUserRepository.findByCompanyAndIsActiveTrue(company);
    }
    
    /**
     * Get store users
     */
    public List<CompanyStoreUser> getStoreUsers(UUID storeId, Authentication authentication) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));
        
        // Verify user has access to the company that owns this store
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        if (store.getCompany() == null || !hasCompanyAccess(user, store.getCompany())) {
            throw new ResourceNotFoundException("You don't have access to this store");
        }
        
        return companyStoreUserRepository.findByStoreAndIsActiveTrue(store);
    }
    
    /**
     * Check if user has reached their subscription limit for company creation
     */
    private void checkSubscriptionLimit(User user) {
        SubscriptionLevel subscriptionLevel = user.getSubscriptionLevel();
        if (subscriptionLevel == null) {
            subscriptionLevel = SubscriptionLevel.FREE;
        }
        
        // Skip check for unlimited plans
        if (subscriptionLevel.isUnlimited()) {
            return;
        }
        
        long currentCompanyCount = companyStoreUserRepository.countCompaniesByFounder(user);
        int maxCompanies = subscriptionLevel.getMaxCompanies();
        
        if (currentCompanyCount >= maxCompanies) {
            throw new PlanLimitExceededException(
                String.format("You have reached the maximum number of companies (%d) allowed for your %s plan. " +
                    "Please upgrade your subscription to create more companies.", 
                    maxCompanies, subscriptionLevel.getDisplayName())
            );
        }
    }
    
    /**
     * Add a role to an existing company membership (many-to-many support)
     */
    public CompanyStoreUserRole addRoleToMembership(UUID companyId, UUID membershipId, CompanyRole role, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        
        // Check if current user has permission to manage roles
        Optional<CompanyRole> currentUserRole = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
        if (currentUserRole.isEmpty() || !currentUserRole.get().canManageUsers()) {
            throw new RuntimeException("You don't have permission to manage roles in this company");
        }
        
        // Find the membership
        CompanyStoreUser membership = companyStoreUserRepository.findById(membershipId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + membershipId));
        
        // Verify membership belongs to the company
        if (!membership.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Membership does not belong to the specified company");
        }
        
        // Check if role already exists
        Optional<CompanyStoreUserRole> existingRole = companyStoreUserRoleRepository
            .findByCompanyStoreUserAndRole(membership, role);
        
        if (existingRole.isPresent()) {
            // If exists but inactive, reactivate it
            if (!existingRole.get().getIsActive()) {
                existingRole.get().restoreRole();
                return companyStoreUserRoleRepository.save(existingRole.get());
            }
            throw new DuplicateResourceException("User already has this role in the company");
        }
        
        // Create new role mapping
        CompanyStoreUserRole roleMapping = new CompanyStoreUserRole(membership, role, username);
        return companyStoreUserRoleRepository.save(roleMapping);
    }
    
    /**
     * Remove a role from a company membership (many-to-many support)
     */
    public void removeRoleFromMembership(UUID companyId, UUID membershipId, CompanyRole role, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username);
        
        Company company = getCompany(companyId, authentication);
        
        // Check if current user has permission to manage roles
        Optional<CompanyRole> currentUserRole = companyStoreUserRepository.findUserRoleInCompany(currentUser, company);
        if (currentUserRole.isEmpty() || !currentUserRole.get().canManageUsers()) {
            throw new RuntimeException("You don't have permission to manage roles in this company");
        }
        
        // Find the membership
        CompanyStoreUser membership = companyStoreUserRepository.findById(membershipId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + membershipId));
        
        // Verify membership belongs to the company
        if (!membership.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Membership does not belong to the specified company");
        }
        
        // Find and revoke the role
        Optional<CompanyStoreUserRole> roleMapping = companyStoreUserRoleRepository
            .findByCompanyStoreUserAndRoleAndIsActiveTrue(membership, role);
        
        if (roleMapping.isEmpty()) {
            throw new ResourceNotFoundException("User does not have this role in the company");
        }
        
        roleMapping.get().revokeRole(username);
        companyStoreUserRoleRepository.save(roleMapping.get());
    }
    
    /**
     * Get all roles for a membership
     */
    public List<CompanyStoreUserRole> getMembershipRoles(UUID membershipId, Authentication authentication) {
        CompanyStoreUser membership = companyStoreUserRepository.findById(membershipId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + membershipId));
        
        // Verify user has access to the company
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        if (!hasCompanyAccess(user, membership.getCompany())) {
            throw new ResourceNotFoundException("You don't have access to this membership");
        }
        
        return companyStoreUserRoleRepository.findByCompanyStoreUser(membership);
    }
}