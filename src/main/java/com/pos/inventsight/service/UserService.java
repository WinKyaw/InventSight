package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.UserRepository;
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

@Service
@Transactional
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    // Authentication Methods
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
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
        user.setCreatedBy("WinKyaw");
        
        // Set email verification status (default to false for new registrations)
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(false);
        }
        
        User savedUser = userRepository.save(user);
        
        // Log activity
        activityLogService.logActivity(
            null, 
            "WinKyaw", 
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
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
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
}