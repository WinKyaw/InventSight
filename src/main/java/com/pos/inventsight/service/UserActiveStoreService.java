package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserActiveStore;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.repository.sql.UserActiveStoreRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserActiveStoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserActiveStoreService.class);
    
    @Autowired
    private UserActiveStoreRepository userActiveStoreRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    /**
     * Get user's currently active store
     */
    public Optional<Store> getUserActiveStore(UUID userId) {
        return userActiveStoreRepository.findByUserId(userId)
            .map(UserActiveStore::getStore);
    }
    
    /**
     * Set user's active store
     * GM+ can switch freely, employees need admin approval
     */
    public void setUserActiveStore(User user, UUID storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        
        // Check if user can access this store
        if (!canUserAccessStore(user, store)) {
            throw new IllegalArgumentException("User does not have access to store: " + store.getStoreName());
        }
        
        Optional<UserActiveStore> existing = userActiveStoreRepository.findByUserId(user.getId());
        
        if (existing.isPresent()) {
            // Update existing
            UserActiveStore activeStore = existing.get();
            activeStore.setStore(store);
            userActiveStoreRepository.save(activeStore);
            
            logger.info("Updated active store for user {} to: {}", user.getUsername(), store.getStoreName());
        } else {
            // Create new
            UserActiveStore activeStore = new UserActiveStore(user, store);
            userActiveStoreRepository.save(activeStore);
            
            logger.info("Set initial active store for user {} to: {}", user.getUsername(), store.getStoreName());
        }
    }
    
    /**
     * Check if user can access a store
     * GM+ can access any store
     * Employees can only access stores they're assigned to
     */
    private boolean canUserAccessStore(User user, Store store) {
        UserRole role = user.getRole();
        
        // ADMIN can access any store (legacy role)
        if (role == UserRole.ADMIN) {
            return true;
        }
        
        // Check via CompanyStoreUser for company-based access
        List<CompanyStoreUser> userStoreRoles = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
        
        for (CompanyStoreUser csu : userStoreRoles) {
            // User has company-level role (FOUNDER, CEO, GENERAL_MANAGER) - can access all stores in company
            if (csu.isCompanyLevel() && csu.getRole().canManageStores()) {
                // Check if store belongs to same company
                if (store.getCompany() != null && store.getCompany().equals(csu.getCompany())) {
                    return true;
                }
            }
            
            // User has specific store assignment
            if (csu.getStore() != null && csu.getStore().equals(store)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get user's active store or throw exception
     */
    public Store getUserActiveStoreOrThrow(UUID userId) {
        return getUserActiveStore(userId)
            .orElseThrow(() -> new IllegalStateException(
                "User has no active store. Please select a store first."
            ));
    }
    
    /**
     * Initialize active store for new user
     * Automatically sets first available store
     */
    public void initializeUserActiveStore(User user) {
        // Check if already has active store
        if (userActiveStoreRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        
        // Get first available store for user
        Store firstStore = getFirstAvailableStore(user);
        
        if (firstStore != null) {
            setUserActiveStore(user, firstStore.getId());
        } else {
            logger.warn("No stores available for user: {}", user.getUsername());
        }
    }
    
    /**
     * Get first available store for user
     */
    private Store getFirstAvailableStore(User user) {
        // Get user's store assignments from CompanyStoreUser
        List<CompanyStoreUser> userStoreRoles = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
        
        for (CompanyStoreUser csu : userStoreRoles) {
            // If user has company-level role, get first store from company
            if (csu.isCompanyLevel() && csu.getRole().canManageStores()) {
                List<Store> companyStores = storeRepository.findByCompany(csu.getCompany());
                if (!companyStores.isEmpty()) {
                    return companyStores.get(0);
                }
            }
            
            // If user has specific store assignment, return that store
            if (csu.getStore() != null) {
                return csu.getStore();
            }
        }
        
        // ADMIN can use any store (legacy role fallback)
        if (user.getRole() == UserRole.ADMIN) {
            return storeRepository.findAll().stream().findFirst().orElse(null);
        }
        
        return null;
    }
}
