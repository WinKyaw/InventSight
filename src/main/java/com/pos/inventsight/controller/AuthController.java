package com.pos.inventsight.controller;

import com.pos.inventsight.dto.AuthResponse;
import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.dto.EmailVerificationRequest;
import com.pos.inventsight.dto.ResendVerificationRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.EmailVerificationService;
import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Value("${inventsight.security.jwt.expiration:86400000}")
    private Long jwtExpirationMs;
    
    // POST /auth/login - User login with JWT token generation
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("🔐 InventSight - Processing login request for: " + loginRequest.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details
            User user = (User) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(user);
            
            // Update last login
            userService.updateLastLogin(user.getId());
            
            // Log authentication activity
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("loginTime", LocalDateTime.now());
            metadata.put("userAgent", "InventSight API");
            metadata.put("ipAddress", "system");
            
            activityLogService.logActivityWithMetadata(
                user.getId().toString(),
                user.getUsername(),
                "USER_LOGIN",
                "AUTHENTICATION",
                "User successfully logged in: " + user.getEmail(),
                metadata
            );
            
            // Create response
            AuthResponse authResponse = new AuthResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                "InventSight",
                jwtExpirationMs
            );
            
            System.out.println("✅ User authenticated successfully: " + user.getEmail());
            return ResponseEntity.ok(authResponse);
            
        } catch (AuthenticationException e) {
            System.out.println("❌ Authentication failed for: " + loginRequest.getEmail());
            
            // Log failed authentication attempt
            activityLogService.logActivity(
                null,
                "WinKyaw",
                "USER_LOGIN_FAILED",
                "AUTHENTICATION",
                "Failed login attempt for email: " + loginRequest.getEmail()
            );
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("Invalid email or password"));
        } catch (Exception e) {
            System.out.println("❌ Login error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Authentication service temporarily unavailable"));
        }
    }
    
    // POST /auth/register - User registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        System.out.println("🔐 InventSight - Processing registration request for: " + registerRequest.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            // Create new user
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setRole(UserRole.USER); // Default role
            
            User savedUser = userService.createUser(user);
            
            // Generate JWT token for immediate login
            String jwt = jwtUtils.generateJwtToken(savedUser);
            
            // Log registration activity
            activityLogService.logActivity(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                "USER_REGISTERED",
                "AUTHENTICATION",
                "New user registered and authenticated: " + savedUser.getEmail()
            );
            
            // Create response
            AuthResponse authResponse = new AuthResponse(
                jwt,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole().name(),
                "InventSight",
                jwtExpirationMs
            );
            
            System.out.println("✅ User registered successfully: " + savedUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (DuplicateResourceException e) {
            System.out.println("❌ Registration failed - duplicate resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthResponse(e.getMessage()));
                
        } catch (Exception e) {
            System.out.println("❌ Registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Registration service temporarily unavailable"));
        }
    }
    
    // GET /auth/check-email - Check if email already exists
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam("email") String email) {
        System.out.println("📧 InventSight - Checking email availability for: " + email);
        
        try {
            boolean emailExists = userService.emailExists(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("exists", emailExists);
            response.put("available", !emailExists);
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ Email availability checked: " + email + " (exists: " + emailExists + ")");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Email check error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Email availability check service temporarily unavailable"));
        }
    }
    
    // POST /auth/verify-email - Email verification endpoint
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationRequest verificationRequest) {
        System.out.println("📧 InventSight - Processing email verification for: " + verificationRequest.getEmail());
        
        try {
            boolean isVerified = emailVerificationService.verifyEmail(
                verificationRequest.getToken(), 
                verificationRequest.getEmail()
            );
            
            if (isVerified) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Email verified successfully");
                response.put("email", verificationRequest.getEmail());
                response.put("timestamp", LocalDateTime.now());
                response.put("system", "InventSight");
                
                System.out.println("✅ Email verified successfully: " + verificationRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Invalid or expired verification token"));
            }
            
        } catch (Exception e) {
            System.out.println("❌ Email verification error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Email verification service temporarily unavailable"));
        }
    }
    
    // POST /auth/resend-verification - Resend verification email
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest resendRequest) {
        System.out.println("📧 InventSight - Resending verification email for: " + resendRequest.getEmail());
        
        try {
            // Check if user exists
            if (!userService.emailExists(resendRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("User not found with this email address"));
            }
            
            // Check if user is already verified
            User user = userService.getUserByEmail(resendRequest.getEmail());
            if (user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Email is already verified"));
            }
            
            // Check if there's already a valid token
            if (emailVerificationService.hasValidToken(resendRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthResponse("A verification email was already sent recently. Please check your inbox."));
            }
            
            // Generate new token and send email
            String token = emailVerificationService.generateVerificationToken(resendRequest.getEmail());
            emailVerificationService.sendVerificationEmail(resendRequest.getEmail(), token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification email sent successfully");
            response.put("email", resendRequest.getEmail());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ Verification email resent successfully: " + resendRequest.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthResponse("User not found with this email address"));
        } catch (Exception e) {
            System.out.println("❌ Resend verification error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Verification email service temporarily unavailable"));
        }
    }
    
    // POST /auth/refresh - Token refresh endpoint
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        System.out.println("🔄 InventSight - Processing token refresh request");
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            String headerAuth = request.getHeader("Authorization");
            
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String jwt = headerAuth.substring(7);
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    User user = userService.getUserByEmail(username);
                    
                    // Generate new token
                    String newJwt = jwtUtils.generateJwtToken(user);
                    
                    // Log refresh activity
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "TOKEN_REFRESHED",
                        "AUTHENTICATION",
                        "JWT token refreshed for user: " + user.getEmail()
                    );
                    
                    AuthResponse authResponse = new AuthResponse(
                        newJwt,
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole().name(),
                        "InventSight",
                        jwtExpirationMs
                    );
                    
                    System.out.println("✅ Token refreshed successfully for: " + user.getEmail());
                    return ResponseEntity.ok(authResponse);
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("Invalid or expired token"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Authorization header missing or invalid"));
            }
            
        } catch (Exception e) {
            System.out.println("❌ Token refresh error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Token refresh service temporarily unavailable"));
        }
    }
    
    // GET /auth/me - Get current user profile
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        System.out.println("👤 InventSight - Fetching current user profile");
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            String headerAuth = request.getHeader("Authorization");
            
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String jwt = headerAuth.substring(7);
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    User user = userService.getUserByEmail(username);
                    
                    // Create user profile response (without token for security)
                    AuthResponse userProfile = new AuthResponse("User profile retrieved");
                    userProfile.setId(user.getId());
                    userProfile.setUsername(user.getUsername());
                    userProfile.setEmail(user.getEmail());
                    userProfile.setFullName(user.getFullName());
                    userProfile.setRole(user.getRole().name());
                    userProfile.setSystem("InventSight");
                    
                    System.out.println("✅ User profile retrieved for: " + user.getEmail());
                    return ResponseEntity.ok(userProfile);
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("Invalid or expired token"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Authorization header missing or invalid"));
            }
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthResponse("User not found"));
        } catch (Exception e) {
            System.out.println("❌ Get current user error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Profile service temporarily unavailable"));
        }
    }
    
    // POST /auth/logout - Logout endpoint
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        System.out.println("🚪 InventSight - Processing logout request");
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            String headerAuth = request.getHeader("Authorization");
            
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String jwt = headerAuth.substring(7);
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    User user = userService.getUserByEmail(username);
                    
                    // Clear security context
                    SecurityContextHolder.clearContext();
                    
                    // Log logout activity
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "USER_LOGOUT",
                        "AUTHENTICATION",
                        "User logged out: " + user.getEmail()
                    );
                    
                    System.out.println("✅ User logged out successfully: " + user.getEmail());
                    return ResponseEntity.ok(new AuthResponse("User logged out successfully"));
                }
            }
            
            // Even if token is invalid, we still return success for logout
            return ResponseEntity.ok(new AuthResponse("Logout completed"));
            
        } catch (Exception e) {
            System.out.println("❌ Logout error: " + e.getMessage());
            // Always return success for logout to avoid revealing information
            return ResponseEntity.ok(new AuthResponse("Logout completed"));
        }
    }
}