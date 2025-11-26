package com.pos.inventsight.service;

import com.pos.inventsight.dto.StoreRequest;
import com.pos.inventsight.dto.StoreResponse;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.tenant.TenantContext;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private com.pos.inventsight.repository.sql.CompanyRepository companyRepository;

    /**
     * Create a new store for the authenticated user
     */
    public StoreResponse createStore(StoreRequest storeRequest, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        // Check if store name already exists for this user
        if (storeRepository.existsByStoreName(storeRequest.getStoreName())) {
            throw new DuplicateResourceException("Store with name '" + storeRequest.getStoreName() + "' already exists");
        }
        
        // Get current tenant context and link store to company
        String tenantId = TenantContext.getCurrentTenant();
        final UUID companyId;
        
        if (tenantId != null && tenantId.startsWith("company_")) {
            // Extract company UUID from tenant context
            String uuidPart = tenantId.substring("company_".length());
            String uuidString = uuidPart.replace("_", "-");
            companyId = UUID.fromString(uuidString);
        } else {
            companyId = null;
        }
        
        // Create new store
        Store store = new Store();
        store.setStoreName(storeRequest.getStoreName());
        store.setDescription(storeRequest.getDescription());
        store.setAddress(storeRequest.getAddress());
        store.setCity(storeRequest.getCity());
        store.setState(storeRequest.getState());
        store.setPostalCode(storeRequest.getPostalCode());
        store.setCountry(storeRequest.getCountry());
        store.setPhone(storeRequest.getPhone());
        store.setEmail(storeRequest.getEmail());
        store.setWebsite(storeRequest.getWebsite());
        store.setTaxId(storeRequest.getTaxId());
        store.setCreatedBy(username);
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedAt(LocalDateTime.now());
        store.setIsActive(true);
        
        // Link store to company if tenant context is set
        if (companyId != null) {
            com.pos.inventsight.model.sql.Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
            store.setCompany(company);
            System.out.println("🏢 Linking store to company: " + company.getName() + " (ID: " + companyId + ")");
        }
        
        Store savedStore = storeRepository.save(store);
        
        // Create user-store role mapping as OWNER
        UserStoreRole userStoreRole = new UserStoreRole(user, savedStore, UserRole.OWNER, username);
        userStoreRoleRepository.save(userStoreRole);
        
        // Log activity
        activityLogService.logActivity(
            user.getId().toString(),
            username,
            "STORE_CREATED",
            "STORE",
            "Created store: " + savedStore.getStoreName() + " (ID: " + savedStore.getId() + ")"
        );
        
        System.out.println("🏪 Store created: " + savedStore.getStoreName() + " by " + username);
        
        return new StoreResponse(savedStore);
    }
    
    /**
     * Get all stores for the authenticated user
     */
    public List<StoreResponse> getUserStores(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(user);
        
        return userStoreRoles.stream()
                .map(role -> new StoreResponse(role.getStore()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get a specific store by ID if user has access
     */
    public StoreResponse getStore(UUID storeId, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        
        // Check if user has access to this store
        boolean hasAccess = userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(user, store).isPresent();
        if (!hasAccess) {
            throw new ResourceNotFoundException("Store not found or access denied");
        }
        
        return new StoreResponse(store);
    }
    
    /**
     * Activate/set a store as the current tenant context for the user
     */
    public StoreResponse activateStore(UUID storeId, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        
        // Check if user has access to this store
        UserStoreRole userStoreRole = userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(user, store)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found or access denied"));
        
        // Set the tenant context to the user's UUID (this is how the system links tenant to stores)
        TenantContext.setCurrentTenant(user.getId().toString());
        
        // Log activity
        activityLogService.logActivity(
            user.getId().toString(),
            username,
            "STORE_ACTIVATED",
            "STORE",
            "Activated store: " + store.getStoreName() + " (ID: " + store.getId() + ") for tenant: " + user.getId()
        );
        
        System.out.println("🎯 Store activated: " + store.getStoreName() + " for user " + username + " (tenant: " + user.getId() + ")");
        
        return new StoreResponse(store);
    }
    
    /**
     * Update store information
     */
    public StoreResponse updateStore(UUID storeId, StoreRequest storeRequest, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        
        // Check if user has owner or co-owner access
        UserStoreRole userStoreRole = userStoreRoleRepository.findByUserAndStoreAndIsActiveTrue(user, store)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found or access denied"));
        
        if (!userStoreRole.isOwnerOrCoOwner()) {
            throw new IllegalArgumentException("Only store owners can update store information");
        }
        
        // Update store fields
        if (storeRequest.getStoreName() != null && !storeRequest.getStoreName().equals(store.getStoreName())) {
            if (storeRepository.existsByStoreName(storeRequest.getStoreName())) {
                throw new DuplicateResourceException("Store with name '" + storeRequest.getStoreName() + "' already exists");
            }
            store.setStoreName(storeRequest.getStoreName());
        }
        
        if (storeRequest.getDescription() != null) {
            store.setDescription(storeRequest.getDescription());
        }
        if (storeRequest.getAddress() != null) {
            store.setAddress(storeRequest.getAddress());
        }
        if (storeRequest.getCity() != null) {
            store.setCity(storeRequest.getCity());
        }
        if (storeRequest.getState() != null) {
            store.setState(storeRequest.getState());
        }
        if (storeRequest.getPostalCode() != null) {
            store.setPostalCode(storeRequest.getPostalCode());
        }
        if (storeRequest.getCountry() != null) {
            store.setCountry(storeRequest.getCountry());
        }
        if (storeRequest.getPhone() != null) {
            store.setPhone(storeRequest.getPhone());
        }
        if (storeRequest.getEmail() != null) {
            store.setEmail(storeRequest.getEmail());
        }
        if (storeRequest.getWebsite() != null) {
            store.setWebsite(storeRequest.getWebsite());
        }
        if (storeRequest.getTaxId() != null) {
            store.setTaxId(storeRequest.getTaxId());
        }
        
        store.setUpdatedBy(username);
        store.setUpdatedAt(LocalDateTime.now());
        
        Store savedStore = storeRepository.save(store);
        
        // Log activity
        activityLogService.logActivity(
            user.getId().toString(),
            username,
            "STORE_UPDATED",
            "STORE",
            "Updated store: " + savedStore.getStoreName() + " (ID: " + savedStore.getId() + ")"
        );
        
        return new StoreResponse(savedStore);
    }
    
    /**
     * Get current active store based on tenant context
     */
    public StoreResponse getCurrentStore() {
        Store currentStore = userService.getCurrentUserStore();
        if (currentStore == null) {
            throw new ResourceNotFoundException("No active store found in current tenant context");
        }
        return new StoreResponse(currentStore);
    }
    
    /**
     * Helper method to ensure user has an active store (for auto-created stores during registration)
     */
    public void ensureUserHasActiveStore(User user) {
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(user);
        
        if (userStoreRoles.isEmpty()) {
            System.out.println("⚠️ No active store found for user: " + user.getUsername() + ", creating default store");
            
            // Get current tenant context and link store to company
            String tenantId = TenantContext.getCurrentTenant();
            final UUID companyId;
            
            if (tenantId != null && tenantId.startsWith("company_")) {
                // Extract company UUID from tenant context
                String uuidPart = tenantId.substring("company_".length());
                String uuidString = uuidPart.replace("_", "-");
                companyId = UUID.fromString(uuidString);
            } else {
                companyId = null;
            }
            
            // Auto-create default store for user
            Store defaultStore = new Store();
            defaultStore.setStoreName("My Store");
            defaultStore.setDescription("Default store for " + user.getFirstName() + " " + user.getLastName());
            defaultStore.setCreatedBy(user.getUsername());
            defaultStore.setCreatedAt(LocalDateTime.now());
            defaultStore.setUpdatedAt(LocalDateTime.now());
            defaultStore.setIsActive(true);
            
            // Link store to company if tenant context is set
            if (companyId != null) {
                com.pos.inventsight.model.sql.Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
                defaultStore.setCompany(company);
                System.out.println("🏢 Auto-created store linked to company: " + company.getName() + " (ID: " + companyId + ")");
            }
            
            Store savedStore = storeRepository.save(defaultStore);
            
            // Create user-store role mapping as OWNER
            UserStoreRole userStoreRole = new UserStoreRole(user, savedStore, UserRole.OWNER, user.getUsername());
            userStoreRoleRepository.save(userStoreRole);
            
            System.out.println("🏪 Default store auto-created: " + savedStore.getStoreName() + " (ID: " + savedStore.getId() + ") for user: " + user.getUsername());
        }
    }
}