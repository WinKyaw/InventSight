package com.pos.inventsight.controller;

import com.pos.inventsight.dto.AuthResponse;
import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.dto.EmailVerificationRequest;
import com.pos.inventsight.dto.ResendVerificationRequest;
import com.pos.inventsight.dto.StructuredAuthResponse;
import com.pos.inventsight.dto.UserResponse;
import com.pos.inventsight.dto.TokenResponse;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.EmailVerificationService;
import com.pos.inventsight.service.PasswordValidationService;
import com.pos.inventsight.service.RateLimitingService;
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
    
    @Autowired
    private PasswordValidationService passwordValidationService;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
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
    
    // POST /auth/login/v2 - User login with structured response (frontend compatibility)
    @PostMapping("/login/v2")
    public ResponseEntity<?> authenticateUserStructured(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("🔐 InventSight - Processing structured login request for: " + loginRequest.getEmail());
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
            String accessToken = jwtUtils.generateJwtToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);
            
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
                "USER_LOGIN_V2",
                "AUTHENTICATION",
                "User successfully logged in (structured): " + user.getEmail(),
                metadata
            );
            
            // Create structured response
            UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole().name()
            );
            
            TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken,
                (long) jwtUtils.getJwtExpirationMs(),
                (long) jwtUtils.getJwtRefreshExpirationMs()
            );
            
            StructuredAuthResponse authResponse = new StructuredAuthResponse(
                userResponse,
                tokenResponse,
                "Login successful"
            );
            
            System.out.println("✅ User authenticated successfully (structured): " + user.getEmail());
            return ResponseEntity.ok(authResponse);
            
        } catch (AuthenticationException e) {
            System.out.println("❌ Authentication failed for: " + loginRequest.getEmail());
            
            // Log failed authentication attempt
            activityLogService.logActivity(
                null,
                "WinKyaw",
                "USER_LOGIN_FAILED_V2",
                "AUTHENTICATION",
                "Failed login attempt for email (structured): " + loginRequest.getEmail()
            );
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new StructuredAuthResponse("Invalid email or password", false));
        } catch (Exception e) {
            System.out.println("❌ Login error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new StructuredAuthResponse("Authentication service temporarily unavailable", false));
        }
    }
    
    // POST /auth/register - User registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, 
                                         HttpServletRequest request) {
        System.out.println("🔐 InventSight - Processing registration request for: " + registerRequest.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check rate limiting
            if (!rateLimitingService.isRegistrationAllowed(clientIp, registerRequest.getEmail())) {
                RateLimitingService.RateLimitStatus rateLimitStatus = 
                    rateLimitingService.getRateLimitStatus(clientIp, registerRequest.getEmail(), "registration");
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Too many registration attempts. Please try again later.");
                errorResponse.put("attempts", rateLimitStatus.getAttempts());
                errorResponse.put("maxAttempts", rateLimitStatus.getMaxAttempts());
                errorResponse.put("resetTime", rateLimitStatus.getResetTime());
                errorResponse.put("timestamp", LocalDateTime.now());
                
                System.out.println("❌ Registration rate limited for: " + registerRequest.getEmail());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
            
            // Record registration attempt
            rateLimitingService.recordRegistrationAttempt(clientIp, registerRequest.getEmail());
            
            // Validate password strength
            PasswordValidationService.PasswordValidationResult passwordValidation = 
                passwordValidationService.validatePassword(registerRequest.getPassword());
            
            if (!passwordValidation.isValid()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Password does not meet security requirements");
                errorResponse.put("errors", passwordValidation.getErrors());
                errorResponse.put("strengthScore", passwordValidation.getStrengthScore());
                errorResponse.put("strength", passwordValidation.getStrength());
                errorResponse.put("timestamp", LocalDateTime.now());
                
                System.out.println("❌ Registration failed - weak password for: " + registerRequest.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
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
    
    // POST /auth/signup - User signup endpoint (frontend compatibility alias for register)
    @PostMapping("/signup")
    public ResponseEntity<?> signupUser(@Valid @RequestBody RegisterRequest signupRequest, 
                                       HttpServletRequest request) {
        System.out.println("🔐 InventSight - Processing signup request for: " + signupRequest.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check rate limiting
            if (!rateLimitingService.isRegistrationAllowed(clientIp, signupRequest.getEmail())) {
                RateLimitingService.RateLimitStatus rateLimitStatus = 
                    rateLimitingService.getRateLimitStatus(clientIp, signupRequest.getEmail(), "registration");
                
                StructuredAuthResponse errorResponse = new StructuredAuthResponse(
                    "Too many signup attempts. Please try again later.", false);
                
                System.out.println("❌ Signup rate limited for: " + signupRequest.getEmail());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
            
            // Record registration attempt
            rateLimitingService.recordRegistrationAttempt(clientIp, signupRequest.getEmail());
            
            // Validate password strength
            PasswordValidationService.PasswordValidationResult passwordValidation = 
                passwordValidationService.validatePassword(signupRequest.getPassword());
            
            if (!passwordValidation.isValid()) {
                StructuredAuthResponse errorResponse = new StructuredAuthResponse(
                    "Password does not meet security requirements", false);
                
                System.out.println("❌ Signup failed - weak password for: " + signupRequest.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Create new user
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(signupRequest.getPassword());
            user.setFirstName(signupRequest.getFirstName());
            user.setLastName(signupRequest.getLastName());
            user.setRole(UserRole.USER); // Default role
            
            User savedUser = userService.createUser(user);
            
            // Generate JWT tokens
            String accessToken = jwtUtils.generateJwtToken(savedUser);
            String refreshToken = jwtUtils.generateRefreshToken(savedUser);
            
            // Log registration activity
            activityLogService.logActivity(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                "USER_SIGNUP",
                "AUTHENTICATION",
                "New user signed up and authenticated: " + savedUser.getEmail()
            );
            
            // Create structured response
            UserResponse userResponse = new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getUsername(),
                savedUser.getRole().name()
            );
            
            TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken,
                (long) jwtUtils.getJwtExpirationMs(),
                (long) jwtUtils.getJwtRefreshExpirationMs()
            );
            
            StructuredAuthResponse authResponse = new StructuredAuthResponse(
                userResponse,
                tokenResponse,
                "Signup successful"
            );
            
            System.out.println("✅ User signed up successfully: " + savedUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (DuplicateResourceException e) {
            System.out.println("❌ Signup failed - duplicate resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new StructuredAuthResponse(e.getMessage(), false));
                
        } catch (Exception e) {
            System.out.println("❌ Signup error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new StructuredAuthResponse("Signup service temporarily unavailable", false));
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
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest resendRequest,
                                              HttpServletRequest request) {
        System.out.println("📧 InventSight - Resending verification email for: " + resendRequest.getEmail());
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check rate limiting for email verification
            if (!rateLimitingService.isEmailVerificationAllowed(clientIp, resendRequest.getEmail())) {
                RateLimitingService.RateLimitStatus rateLimitStatus = 
                    rateLimitingService.getRateLimitStatus(clientIp, resendRequest.getEmail(), "email-verification");
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Too many email verification attempts. Please try again later.");
                errorResponse.put("attempts", rateLimitStatus.getAttempts());
                errorResponse.put("maxAttempts", rateLimitStatus.getMaxAttempts());
                errorResponse.put("resetTime", rateLimitStatus.getResetTime());
                errorResponse.put("timestamp", LocalDateTime.now());
                
                System.out.println("❌ Email verification rate limited for: " + resendRequest.getEmail());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
            
            // Record email verification attempt
            rateLimitingService.recordEmailVerificationAttempt(clientIp, resendRequest.getEmail());
            
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
    
    // POST /auth/validate-password - Password strength validation endpoint
    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(@RequestBody Map<String, String> request) {
        System.out.println("🔐 InventSight - Validating password strength");
        
        try {
            String password = request.get("password");
            
            if (password == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Password is required");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(response);
            }
            
            PasswordValidationService.PasswordValidationResult validation = 
                passwordValidationService.validatePassword(password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", validation.isValid());
            response.put("strengthScore", validation.getStrengthScore());
            response.put("strength", validation.getStrength());
            response.put("errors", validation.getErrors());
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ Password validation completed - strength: " + validation.getStrength());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Password validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Password validation service temporarily unavailable"));
        }
    }
    
    // Helper method to get client IP address
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For may contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
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