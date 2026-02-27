package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.ChangePasswordRequest;
import com.pos.inventsight.dto.UpdateProfileRequest;
import com.pos.inventsight.dto.UserProfileResponse;
import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.EmployeeRepository;
import com.pos.inventsight.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * GET /api/users/me - Get current user's own profile
     * Accessible by ALL authenticated users (employees and GM+)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("👤 Profile requested by: {}", username);

            User user = userService.getUserByUsername(username);
            UserProfileResponse profile = buildProfileResponse(user);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("❌ Error fetching profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch profile"));
        }
    }

    /**
     * PUT /api/users/me - Update current user's own profile (email, phone)
     * Accessible by ALL authenticated users (employees and GM+)
     * Employees can ONLY update their own profile
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("✏️ Profile update requested by: {}", username);

            User user = userService.getUserByUsername(username);

            // Update allowed fields only
            boolean updated = false;

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                String newEmail = request.getEmail().trim().toLowerCase();

                // Check email uniqueness (exclude current user)
                if (!newEmail.equals(user.getEmail())) {
                    boolean emailExists = userService.existsByEmail(newEmail);
                    if (emailExists) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new ApiResponse(false, "Email address is already in use"));
                    }
                    user.setEmail(newEmail);
                    // If username matches old email pattern, sync username too
                    if (user.getUsername().contains("@inventsight.com")) {
                        user.setUsername(newEmail);
                    }
                    updated = true;
                }
            }

            if (request.getPhone() != null) {
                user.setPhone(request.getPhone().trim());
                updated = true;
            }

            if (updated) {
                user.setUpdatedAt(LocalDateTime.now());
                userService.saveUser(user);

                // Sync email/phone to associated employee record if exists
                Optional<Employee> employeeOpt = employeeRepository.findByUser(user);
                if (employeeOpt.isPresent()) {
                    Employee emp = employeeOpt.get();
                    if (request.getEmail() != null && !request.getEmail().isBlank()) {
                        emp.setEmail(request.getEmail().trim().toLowerCase());
                    }
                    if (request.getPhone() != null) {
                        emp.setPhoneNumber(user.getPhone());
                    }
                    emp.setUpdatedAt(LocalDateTime.now());
                    employeeRepository.save(emp);
                }
            }

            UserProfileResponse profile = buildProfileResponse(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", profile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error updating profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * POST /api/users/me/change-password - Change own password
     * Accessible by ALL authenticated users
     * Requires current password verification for security
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("🔐 Password change requested by: {}", username);

            // Validate new passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "New password and confirmation do not match"));
            }

            // Validate new password is different from current
            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "New password must be different from current password"));
            }

            User user = userService.getUserByUsername(username);

            // Verify current password — CRITICAL security check
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                logger.warn("⚠️ Failed password change attempt for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "Current password is incorrect"));
            }

            // Encode with Argon2id (via configured PasswordEncoder bean)
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userService.saveUser(user);

            logger.info("✅ Password changed successfully for user: {}", username);

            return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
        } catch (Exception e) {
            logger.error("❌ Error changing password: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to change password"));
        }
    }

    /**
     * GET /api/users/{id}/profile - GM+ can view any employee's profile
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getUserProfile(
            @PathVariable("id") String userId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User requester = userService.getUserByUsername(username);

            // Security: only GM+ can view other users' profiles
            if (!isGMPlus(requester)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Access denied. GM+ role required to view other users."));
            }

            User targetUser = userService.getUserById(UUID.fromString(userId));
            UserProfileResponse profile = buildProfileResponse(targetUser);

            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Invalid user ID format"));
        } catch (Exception e) {
            logger.error("❌ Error fetching user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch user profile"));
        }
    }

    // ---- Private helpers ----

    private UserProfileResponse buildProfileResponse(User user) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setEmail(user.getEmail());
        profile.setUsername(user.getUsername());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole() != null ? user.getRole().name() : null);

        // Enrich with employee-specific data if available
        Optional<Employee> employeeOpt = employeeRepository.findByUser(user);
        if (employeeOpt.isPresent()) {
            Employee emp = employeeOpt.get();
            profile.setEmployeeTitle(emp.getTitle());
            profile.setDepartment(emp.getDepartment());
            if (emp.getStore() != null) {
                profile.setStoreName(emp.getStore().getStoreName());
            }
            if (emp.getCompany() != null) {
                profile.setCompanyName(emp.getCompany().getName());
            }
        }

        return profile;
    }

    private boolean isGMPlus(User user) {
        if (user.getRole() == null) return false;
        String role = user.getRole().name().toUpperCase();
        return role.equals("OWNER") || role.equals("FOUNDER") || role.equals("CO_OWNER") ||
               role.equals("MANAGER") || role.equals("ADMIN") || role.equals("GENERAL_MANAGER") ||
               role.equals("CEO");
    }
}
