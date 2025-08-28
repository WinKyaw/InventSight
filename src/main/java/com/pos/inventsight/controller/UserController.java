package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.UserProfileRequest;
import com.pos.inventsight.dto.UserSettingsRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserProfile;
import com.pos.inventsight.model.sql.UserSettings;
import com.pos.inventsight.repository.sql.UserProfileRepository;
import com.pos.inventsight.repository.sql.UserSettingsRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Value("${app.upload.dir:${user.dir}/uploads}")
    private String uploadDir;
    
    // GET /api/users/profile - Get detailed user profile
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("👤 InventSight - Fetching profile for user: " + username);
            
            UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                    .orElse(new UserProfile(user));
            
            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId())
                    .orElse(new UserSettings(user));
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("profile", userProfile);
            response.put("settings", userSettings);
            response.put("permissions", user.getRole().name());
            response.put("isAdmin", user.getRole().name().equals("ADMIN"));
            response.put("fullName", user.getFirstName() + " " + user.getLastName());
            response.put("profileComplete", isProfileComplete(userProfile));
            
            System.out.println("✅ Retrieved profile for user: " + username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching user profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching user profile: " + e.getMessage()));
        }
    }
    
    // PUT /api/users/profile - Update user profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserProfileRequest profileRequest,
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("👤 InventSight - Updating profile for user: " + username);
            
            UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                    .orElse(new UserProfile(user));
            
            // Update profile fields
            if (profileRequest.getBio() != null) {
                userProfile.setBio(profileRequest.getBio());
            }
            if (profileRequest.getDateOfBirth() != null) {
                userProfile.setDateOfBirth(profileRequest.getDateOfBirth());
            }
            if (profileRequest.getAddress() != null) {
                userProfile.setAddress(profileRequest.getAddress());
            }
            if (profileRequest.getCity() != null) {
                userProfile.setCity(profileRequest.getCity());
            }
            if (profileRequest.getState() != null) {
                userProfile.setState(profileRequest.getState());
            }
            if (profileRequest.getPostalCode() != null) {
                userProfile.setPostalCode(profileRequest.getPostalCode());
            }
            if (profileRequest.getCountry() != null) {
                userProfile.setCountry(profileRequest.getCountry());
            }
            if (profileRequest.getDepartment() != null) {
                userProfile.setDepartment(profileRequest.getDepartment());
            }
            if (profileRequest.getJobTitle() != null) {
                userProfile.setJobTitle(profileRequest.getJobTitle());
            }
            if (profileRequest.getHireDate() != null) {
                userProfile.setHireDate(profileRequest.getHireDate());
            }
            if (profileRequest.getManager() != null) {
                userProfile.setManager(profileRequest.getManager());
            }
            if (profileRequest.getEmergencyContactName() != null) {
                userProfile.setEmergencyContactName(profileRequest.getEmergencyContactName());
            }
            if (profileRequest.getEmergencyContactPhone() != null) {
                userProfile.setEmergencyContactPhone(profileRequest.getEmergencyContactPhone());
            }
            if (profileRequest.getEmergencyContactRelationship() != null) {
                userProfile.setEmergencyContactRelationship(profileRequest.getEmergencyContactRelationship());
            }
            
            userProfile.setUpdatedAt(LocalDateTime.now());
            UserProfile savedProfile = userProfileRepository.save(userProfile);
            
            // Log activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "PROFILE_UPDATED",
                "User",
                "Updated user profile for: " + user.getUsername()
            );
            
            System.out.println("✅ Updated profile for user: " + username);
            return ResponseEntity.ok(savedProfile);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating user profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating user profile: " + e.getMessage()));
        }
    }
    
    // POST /api/users/profile/avatar - Upload profile picture
    @PostMapping("/profile/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                        Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("📷 InventSight - Uploading avatar for user: " + username);
            
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Please select a file to upload"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Only image files are allowed"));
            }
            
            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "File size must be less than 5MB"));
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "avatars");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String fileName = user.getId() + "_" + UUID.randomUUID().toString() + 
                             getFileExtension(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(fileName);
            
            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Update user profile with avatar URL
            UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                    .orElse(new UserProfile(user));
            
            String avatarUrl = "/uploads/avatars/" + fileName;
            userProfile.setAvatarUrl(avatarUrl);
            userProfile.setUpdatedAt(LocalDateTime.now());
            
            UserProfile savedProfile = userProfileRepository.save(userProfile);
            
            // Log activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "AVATAR_UPLOADED",
                "User",
                "Uploaded avatar for: " + user.getUsername()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Avatar uploaded successfully");
            response.put("avatarUrl", avatarUrl);
            response.put("fileName", fileName);
            
            System.out.println("✅ Avatar uploaded for user: " + username + " -> " + fileName);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            System.err.println("❌ Error uploading avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error uploading avatar: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error uploading avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error uploading avatar: " + e.getMessage()));
        }
    }
    
    // GET /api/users/settings - Get user settings
    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("⚙️ InventSight - Fetching settings for user: " + username);
            
            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId())
                    .orElse(new UserSettings(user));
            
            System.out.println("✅ Retrieved settings for user: " + username);
            return ResponseEntity.ok(userSettings);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching user settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error fetching user settings: " + e.getMessage()));
        }
    }
    
    // PUT /api/users/settings - Update user preferences/settings
    @PutMapping("/settings")
    public ResponseEntity<?> updateUserSettings(@Valid @RequestBody UserSettingsRequest settingsRequest,
                                              Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            System.out.println("⚙️ InventSight - Updating settings for user: " + username);
            
            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId())
                    .orElse(new UserSettings(user));
            
            // Update display & UI settings
            if (settingsRequest.getTheme() != null) {
                userSettings.setTheme(UserSettings.Theme.valueOf(settingsRequest.getTheme().toUpperCase()));
            }
            if (settingsRequest.getLanguage() != null) {
                userSettings.setLanguage(settingsRequest.getLanguage());
            }
            if (settingsRequest.getTimezone() != null) {
                userSettings.setTimezone(settingsRequest.getTimezone());
            }
            if (settingsRequest.getDateFormat() != null) {
                userSettings.setDateFormat(settingsRequest.getDateFormat());
            }
            if (settingsRequest.getTimeFormat() != null) {
                userSettings.setTimeFormat(settingsRequest.getTimeFormat());
            }
            if (settingsRequest.getCurrency() != null) {
                userSettings.setCurrency(settingsRequest.getCurrency());
            }
            
            // Update notification settings
            if (settingsRequest.getEmailNotifications() != null) {
                userSettings.setEmailNotifications(settingsRequest.getEmailNotifications());
            }
            if (settingsRequest.getPushNotifications() != null) {
                userSettings.setPushNotifications(settingsRequest.getPushNotifications());
            }
            if (settingsRequest.getSmsNotifications() != null) {
                userSettings.setSmsNotifications(settingsRequest.getSmsNotifications());
            }
            if (settingsRequest.getNotificationFrequency() != null) {
                userSettings.setNotificationFrequency(UserSettings.NotificationFrequency.valueOf(settingsRequest.getNotificationFrequency().toUpperCase()));
            }
            
            // Update inventory settings
            if (settingsRequest.getLowStockThreshold() != null) {
                userSettings.setLowStockThreshold(settingsRequest.getLowStockThreshold());
            }
            if (settingsRequest.getAutoReorder() != null) {
                userSettings.setAutoReorder(settingsRequest.getAutoReorder());
            }
            if (settingsRequest.getDefaultSupplierId() != null) {
                userSettings.setDefaultSupplierId(settingsRequest.getDefaultSupplierId());
            }
            
            // Update calendar settings
            if (settingsRequest.getCalendarView() != null) {
                userSettings.setCalendarView(UserSettings.CalendarView.valueOf(settingsRequest.getCalendarView().toUpperCase()));
            }
            if (settingsRequest.getWorkingHoursStart() != null) {
                userSettings.setWorkingHoursStart(settingsRequest.getWorkingHoursStart());
            }
            if (settingsRequest.getWorkingHoursEnd() != null) {
                userSettings.setWorkingHoursEnd(settingsRequest.getWorkingHoursEnd());
            }
            if (settingsRequest.getWeekendIncluded() != null) {
                userSettings.setWeekendIncluded(settingsRequest.getWeekendIncluded());
            }
            
            // Update security settings
            if (settingsRequest.getTwoFactorEnabled() != null) {
                userSettings.setTwoFactorEnabled(settingsRequest.getTwoFactorEnabled());
            }
            if (settingsRequest.getSessionTimeoutMinutes() != null) {
                userSettings.setSessionTimeoutMinutes(settingsRequest.getSessionTimeoutMinutes());
            }
            if (settingsRequest.getPasswordChangeRequired() != null) {
                userSettings.setPasswordChangeRequired(settingsRequest.getPasswordChangeRequired());
            }
            
            // Update privacy settings
            if (settingsRequest.getProfileVisibility() != null) {
                userSettings.setProfileVisibility(UserSettings.ProfileVisibility.valueOf(settingsRequest.getProfileVisibility().toUpperCase()));
            }
            if (settingsRequest.getActivityTracking() != null) {
                userSettings.setActivityTracking(settingsRequest.getActivityTracking());
            }
            if (settingsRequest.getDataSharing() != null) {
                userSettings.setDataSharing(settingsRequest.getDataSharing());
            }
            
            userSettings.setUpdatedAt(LocalDateTime.now());
            UserSettings savedSettings = userSettingsRepository.save(userSettings);
            
            // Log activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "SETTINGS_UPDATED",
                "User",
                "Updated user settings for: " + user.getUsername()
            );
            
            System.out.println("✅ Updated settings for user: " + username);
            return ResponseEntity.ok(savedSettings);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating user settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Error updating user settings: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private boolean isProfileComplete(UserProfile profile) {
        return profile.getBio() != null && !profile.getBio().trim().isEmpty() &&
               profile.getDateOfBirth() != null &&
               profile.getAddress() != null && !profile.getAddress().trim().isEmpty() &&
               profile.getCity() != null && !profile.getCity().trim().isEmpty() &&
               profile.getEmergencyContactName() != null && !profile.getEmergencyContactName().trim().isEmpty() &&
               profile.getEmergencyContactPhone() != null && !profile.getEmergencyContactPhone().trim().isEmpty();
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}