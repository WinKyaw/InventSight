package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.repository.sql.StoreRepository;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.tenant.TenantContext;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private UserPreferencesService userPreferencesService;
    
    // Authentication Methods
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Support both username and email for authentication
        // First try username, then try email if username lookup fails
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            user = userRepository.findByEmail(username);
        }
        
        return user.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    // Method to get user by email specifically
    public User getUserByEmail(String email) throws ResourceNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    // Method to get user by username specifically  
    public User getUserByUsername(String username) throws ResourceNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    
    public User createUser(User user) {
        System.out.println("🔐 Creating new user: " + user.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-26 08:47:36");
        System.out.println("👤 Current User: WinKyaw");
        
        // Check for duplicates
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(user.getUsername());
        
        // Set email verification status (default to false for new registrations)
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(false);
        }
        
        // Note: We save the user initially, then update with default_tenant_id after company creation
        // This is necessary because company creation depends on the saved user entity
        User savedUser = userRepository.save(user);
        
        // Auto-create company for new user with founder role
        Company company = new Company();
        company.setName(savedUser.getFirstName() + "'s Company");
        company.setDescription("Default company for " + savedUser.getFirstName() + " " + savedUser.getLastName());
        company.setEmail(savedUser.getEmail()); // Use user's email as company email
        company.setCreatedBy(savedUser.getUsername());
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company.setIsActive(true);
        Company savedCompany = companyRepository.save(company);
        
        // Auto-create default store for the company
        Store defaultStore = new Store();
        defaultStore.setStoreName("My Store");
        defaultStore.setDescription("Default store for " + savedUser.getFirstName() + " " + savedUser.getLastName());
        defaultStore.setCreatedBy(savedUser.getUsername());
        defaultStore.setCreatedAt(LocalDateTime.now());
        defaultStore.setUpdatedAt(LocalDateTime.now());
        defaultStore.setIsActive(true);
        defaultStore.setCompany(savedCompany); // Link store to company
        Store savedStore = storeRepository.save(defaultStore);
        
        // Create company-user relationship with founder role
        CompanyStoreUser companyStoreUser = new CompanyStoreUser(savedCompany, savedUser, CompanyRole.FOUNDER, savedUser.getUsername());
        companyStoreUserRepository.save(companyStoreUser);
        
        // Create legacy user-store role mapping for backward compatibility
        UserStoreRole userStoreRole = new UserStoreRole(savedUser, savedStore, UserRole.OWNER, savedUser.getUsername());
        userStoreRoleRepository.save(userStoreRole);
        
        // Set default tenant to the newly created company for automatic tenant binding
        // Update in single operation to minimize database round-trips
        savedUser.setDefaultTenantId(savedCompany.getId());
        savedUser = userRepository.save(savedUser);
        
        System.out.println("🏢 Company created: " + savedCompany.getName() + " (ID: " + savedCompany.getId() + ")");
        System.out.println("🏪 Default store created: " + savedStore.getStoreName() + " (ID: " + savedStore.getId() + ")");
        System.out.println("👑 User assigned as FOUNDER with company-level access");
        System.out.println("🎯 Default tenant set for automatic login: " + savedCompany.getId());
        
        // Set tenant context for the new user to ensure proper association
        TenantContext.setCurrentTenant(savedUser.getId().toString());
        System.out.println("🎯 Tenant context initialized for new user: " + savedUser.getUsername() + 
                         " (UUID: " + savedUser.getId() + ")");
        
        // Auto-create default user preferences
        userPreferencesService.createDefaultPreferences(savedUser.getId());
        System.out.println("⚙️ Default user preferences created");
        
        // Log activity
        activityLogService.logActivity(
            null, 
            user.getUsername(), 
            "USER_CREATED", 
            "USER", 
            "New user registered: " + user.getEmail()
        );
        
        System.out.println("✅ User created successfully: " + savedUser.getEmail());
        return savedUser;
    }
    
    public User updateUser(UUID userId, User userUpdates) {
        User existingUser = getUserById(userId);
        
        // Update fields
        if (userUpdates.getFirstName() != null) {
            existingUser.setFirstName(userUpdates.getFirstName());
        }
        if (userUpdates.getLastName() != null) {
            existingUser.setLastName(userUpdates.getLastName());
        }
        if (userUpdates.getEmail() != null && !userUpdates.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(userUpdates.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + userUpdates.getEmail());
            }
            existingUser.setEmail(userUpdates.getEmail());
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(existingUser);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(), 
            "WinKyaw", 
            "USER_UPDATED", 
            "USER", 
            "User profile updated"
        );
        
        return updatedUser;
    }
    
    public void updateLastLogin(UUID userId) {
        User user = getUserById(userId);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        System.out.println("📅 Updated last login for user: " + user.getUsername() + " at 2025-08-26 08:47:36");
    }
    
    // CRUD Operations
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
    
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public void deactivateUser(UUID userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(), 
            "WinKyaw", 
            "USER_DEACTIVATED", 
            "USER", 
            "User account deactivated"
        );
    }
    
    // Statistics
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }
    
    public List<User> getRecentActiveUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return userRepository.findActiveUsersSince(since);
    }
    
    // Email and Username availability checks
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // Tenant-related methods
    
    /**
     * Extract UUID from tenant ID (handles both formats)
     * @param tenantId either a raw UUID or company schema name (company_uuid_with_underscores)
     * @return the extracted UUID
     * @throws IllegalArgumentException if format is invalid
     */
    private UUID extractUuidFromTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        // Check if it's a company schema format: "company_uuid-with-underscores"
        if (tenantId.startsWith("company_")) {
            // Extract UUID from company schema name
            // Format: "company_87b6a00e_896a_4f69_b9cd_3349d50c1578"
            // Convert to: "87b6a00e-896a-4f69-b9cd-3349d50c1578"
            String uuidPart = tenantId.substring("company_".length()); // Remove "company_" prefix
            String uuidString = uuidPart.replace("_", "-"); // Replace underscores with hyphens
            return UUID.fromString(uuidString);
        } else {
            // Direct UUID format
            return UUID.fromString(tenantId);
        }
    }
    
    /**
     * Get the current user's primary store based on tenant context
     * Handles both user UUID tenancy and company schema tenancy
     */
    public Store getCurrentUserStore() {
        String tenantId = TenantContext.getCurrentTenant();
        logger.info("🏪 getCurrentUserStore() called with tenant: {}", tenantId);
        
        // If using default tenant, return null (no specific store)
        if (TenantContext.DEFAULT_TENANT.equals(tenantId)) {
            logger.warn("Using default tenant - no specific store available");
            return null;
        }
        
        // Determine which tenancy mode we're in
        User user;
        
        if (tenantId.startsWith("company_")) {
            // Company-based tenancy: Get user from SecurityContext
            logger.debug("Company-based tenancy detected");
            user = getAuthenticatedUser();
            
            if (user == null) {
                throw new ResourceNotFoundException("No authenticated user found in SecurityContext");
            }
            
            logger.info("✅ Using authenticated user from SecurityContext: {} (UUID: {})", 
                       user.getUsername(), user.getId());
            
            // Extract company UUID from tenant ID for verification
            UUID companyUuid;
            try {
                String uuidPart = tenantId.substring("company_".length());
                String uuidString = uuidPart.replace("_", "-");
                companyUuid = UUID.fromString(uuidString);
                logger.debug("Company UUID from tenant: {}", companyUuid);
            } catch (Exception e) {
                throw new ResourceNotFoundException("Invalid company schema format: " + tenantId);
            }
            
            // Verify user has membership in this company
            List<CompanyStoreUser> companyMemberships = companyStoreUserRepository
                .findByUserAndCompanyIdAndIsActiveTrue(user, companyUuid);
            
            if (companyMemberships.isEmpty()) {
                throw new ResourceNotFoundException("User " + user.getUsername() + 
                    " does not have access to company: " + companyUuid);
            }
            
            logger.info("✅ User {} has {} active membership(s) in company {}", 
                       user.getUsername(), companyMemberships.size(), companyUuid);
            
        } else {
            // Legacy user-based tenancy: tenant ID IS the user UUID
            logger.debug("User-based tenancy detected");
            UUID userUuid;
            try {
                userUuid = UUID.fromString(tenantId);
                logger.debug("User UUID from tenant: {}", userUuid);
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Invalid UUID format for tenant: " + tenantId + " - " + e.getMessage());
            }
            
            // Find user by UUID (id)
            user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for tenant: " + tenantId));
            
            logger.info("✅ Found user from tenant UUID: {} (ID: {})", user.getUsername(), user.getId());
        }
        
        // Get user's primary store role
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(user);
        if (userStoreRoles.isEmpty()) {
            throw new ResourceNotFoundException("No active store found for user: " + user.getUsername());
        }
        
        Store store = userStoreRoles.get(0).getStore();
        logger.info("✅ Returning store: {} (ID: {}) for user: {}", 
                   store.getStoreName(), store.getId(), user.getUsername());
        
        return store;
    }
    
    /**
     * Get the currently authenticated user from SecurityContext
     * @return the authenticated User object, or null if not authenticated
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && 
                authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof User) {
                
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            logger.debug("Error getting authenticated user: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get user by UUID string (for compatibility with external APIs)
     */
    public User getUserByUuid(String uuid) {
        UUID userUuid;
        try {
            userUuid = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid UUID format: " + uuid);
        }
        return userRepository.findById(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + uuid));
    }
    
    /**
     * Get user by UUID object (for internal use)
     */
    public User getUserByUuid(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + uuid));
    }
    
    /**
     * Set tenant context for authenticated user
     * This helps ensure that tenant context is properly initialized
     */
    public void setTenantContextForUser(String username) {
        User user = getUserByUsername(username);
        
        // Set tenant context to user's ID (UUID)
        TenantContext.setCurrentTenant(user.getId().toString());
        
        System.out.println("🎯 Tenant context set for user: " + username + " (UUID: " + user.getId() + ")");
    }
    
    /**
     * Ensure user has proper tenant context and active store
     */
    public Store ensureUserTenantContext(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        String username = authentication.getName();
        User user = getUserByUsername(username);
        
        // Set tenant context
        setTenantContextForUser(username);
        
        // Ensure user has at least one active store
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(user);
        if (userStoreRoles.isEmpty()) {
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
            
            // Auto-create default store if none exists
            Store defaultStore = new Store();
            defaultStore.setStoreName("My Store");
            defaultStore.setDescription("Default store for " + user.getFirstName() + " " + user.getLastName());
            defaultStore.setCreatedBy(user.getUsername());
            defaultStore.setCreatedAt(LocalDateTime.now());
            defaultStore.setUpdatedAt(LocalDateTime.now());
            defaultStore.setIsActive(true);
            
            // Link store to company if tenant context is set
            if (companyId != null) {
                Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
                defaultStore.setCompany(company);
                System.out.println("🏢 Auto-created store linked to company: " + company.getName() + " (ID: " + companyId + ")");
            }
            
            Store savedStore = storeRepository.save(defaultStore);
            
            // Create user-store role mapping as OWNER
            UserStoreRole userStoreRole = new UserStoreRole(user, savedStore, UserRole.OWNER, user.getUsername());
            userStoreRoleRepository.save(userStoreRole);
            
            System.out.println("🏪 Auto-created default store for tenant context: " + savedStore.getStoreName() + 
                             " (ID: " + savedStore.getId() + ") for user: " + username);
            
            return savedStore;
        }
        
        // Return the first active store
        return userStoreRoles.get(0).getStore();
    }
}