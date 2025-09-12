package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.UserStoreRole;
import com.pos.inventsight.repository.sql.UserRepository;
import com.pos.inventsight.repository.sql.UserStoreRoleRepository;
import com.pos.inventsight.tenant.TenantContext;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserStoreRoleRepository userStoreRoleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ActivityLogService activityLogService;
    
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
        
        User savedUser = userRepository.save(user);
        
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
    
    public User updateUser(Long userId, User userUpdates) {
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
    
    public void updateLastLogin(Long userId) {
        User user = getUserById(userId);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        System.out.println("📅 Updated last login for user: " + user.getUsername() + " at 2025-08-26 08:47:36");
    }
    
    // CRUD Operations
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
    
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }
    
    public void deactivateUser(Long userId) {
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
     * Get the current user's primary store based on tenant context
     * The tenant ID should correspond to a user's UUID
     */
    public Store getCurrentUserStore() {
        String tenantId = TenantContext.getCurrentTenant();
        
        // If using default tenant, return null (no specific store)
        if (TenantContext.DEFAULT_TENANT.equals(tenantId)) {
            return null;
        }
        
        // Find user by UUID (tenant ID)
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid UUID format for tenant: " + tenantId);
        }
        
        User user = userRepository.findByUuid(tenantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for tenant: " + tenantId));
        
        // Get user's primary store role
        List<UserStoreRole> userStoreRoles = userStoreRoleRepository.findByUserAndIsActiveTrue(user);
        if (userStoreRoles.isEmpty()) {
            throw new ResourceNotFoundException("No active store found for user: " + user.getUsername());
        }
        
        // Return the first active store (in a real implementation, you might want better logic)
        return userStoreRoles.get(0).getStore();
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
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + uuid));
    }
    
    /**
     * Get user by UUID object (for internal use)
     */
    public User getUserByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + uuid));
    }
}