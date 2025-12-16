package com.pos.inventsight.controller;

import com.pos.inventsight.dto.AuthResponse;
import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.dto.EmailVerificationRequest;
import com.pos.inventsight.dto.ResendVerificationRequest;
import com.pos.inventsight.dto.StructuredAuthResponse;
import com.pos.inventsight.dto.UserResponse;
import com.pos.inventsight.dto.TokenResponse;
import com.pos.inventsight.dto.RefreshTokenRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.RefreshToken;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.EmailVerificationService;
import com.pos.inventsight.service.PasswordValidationService;
import com.pos.inventsight.service.RateLimitingService;
import com.pos.inventsight.service.RefreshTokenService;
import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.DuplicateResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * Authentication Controller
 * 
 * Handles all authentication-related endpoints for the InventSight application.
 * This includes user login, registration, email verification, and token management.
 * All endpoints are prefixed with /auth and become /api/auth due to the context path.
 * 
 * This controller is disabled by default (OAuth2-only mode).
 * To enable local authentication, set: inventsight.security.local-login.enabled=true
 * 
 * Key endpoints:
 * - POST /auth/register - User registration with immediate authentication
 * - POST /auth/signup - Alternative registration endpoint for frontend compatibility
 * - POST /auth/login - User authentication with JWT token generation
 * - POST /auth/verify-email - Email verification for new accounts
 * - GET /auth/check-email - Check email availability for registration
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@ConditionalOnProperty(name = "inventsight.security.local-login.enabled", havingValue = "true", matchIfMissing = false)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
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
    
    @Autowired
    private com.pos.inventsight.repository.sql.CompanyRepository companyRepository;
    
    @Autowired
    private com.pos.inventsight.repository.sql.CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private com.pos.inventsight.service.MfaService mfaService;
    
    @Autowired(required = false)
    private RefreshTokenService refreshTokenService;
    
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
            
            // Check email verification status after successful authentication
            // This provides better UX for existing users while maintaining security
            // by not exposing which emails are registered (credentials are validated first)
            if (user.getEmailVerified() == null || !user.getEmailVerified()) {
                System.out.println("❌ Login blocked - email not verified for: " + loginRequest.getEmail());
                
                // Log failed authentication attempt due to unverified email
                activityLogService.logActivity(
                    user.getId().toString(),
                    user.getUsername(),
                    "USER_LOGIN_EMAIL_UNVERIFIED",
                    "AUTHENTICATION",
                    "Login attempt with unverified email: " + loginRequest.getEmail()
                );
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Email not verified. Please verify your email before logging in."));
            }
            
            // Check MFA status and validate code if enabled
            boolean mfaEnabled = mfaService.isMfaEnabled(user);
            if (mfaEnabled) {
                // Get user's preferred MFA delivery method
                com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod preferredMethod = 
                    mfaService.getPreferredDeliveryMethod(user);
                
                // Check if any MFA code is provided
                boolean hasTotpCode = loginRequest.getTotpCode() != null;
                boolean hasOtpCode = loginRequest.getOtpCode() != null && !loginRequest.getOtpCode().isEmpty();
                
                if (!hasTotpCode && !hasOtpCode) {
                    System.out.println("❌ Login blocked - MFA code required for: " + loginRequest.getEmail());
                    
                    // Log MFA required event
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_REQUIRED",
                        "AUTHENTICATION",
                        "Login attempt without MFA code: " + loginRequest.getEmail()
                    );
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "MFA_REQUIRED");
                    errorResponse.put("message", "Multi-factor authentication code is required");
                    errorResponse.put("preferredMethod", preferredMethod.toString());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
                
                // Check rate limiting for MFA login attempts
                if (!rateLimitingService.isMfaVerificationAllowed(user.getEmail())) {
                    System.out.println("❌ Login blocked - MFA rate limit exceeded for: " + loginRequest.getEmail());
                    
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_RATE_LIMITED",
                        "AUTHENTICATION",
                        "MFA login rate limit exceeded: " + loginRequest.getEmail()
                    );
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "MFA_RATE_LIMITED");
                    errorResponse.put("message", "Too many MFA verification attempts. Please try again later.");
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
                }
                
                // Record MFA verification attempt
                rateLimitingService.recordMfaVerificationAttempt(user.getEmail());
                
                boolean codeValid = false;
                String methodUsed = null;
                
                // Verify based on the code type provided
                if (hasOtpCode) {
                    // OTP code provided - verify as email/SMS OTP
                    com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod deliveryMethod = 
                        preferredMethod == com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.SMS
                            ? com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.SMS
                            : com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.EMAIL;
                    
                    codeValid = mfaService.verifyOtpCode(user, loginRequest.getOtpCode(), deliveryMethod);
                    methodUsed = deliveryMethod.toString();
                } else if (hasTotpCode) {
                    // TOTP code provided - verify as TOTP
                    codeValid = mfaService.verifyCode(user, loginRequest.getTotpCode());
                    methodUsed = "TOTP";
                }
                
                if (!codeValid) {
                    System.out.println("❌ Login blocked - invalid MFA code for: " + loginRequest.getEmail());
                    
                    // Log MFA validation failure
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_FAILED",
                        "AUTHENTICATION",
                        "Login attempt with invalid MFA code: " + loginRequest.getEmail()
                    );
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "MFA_INVALID_CODE");
                    errorResponse.put("message", "Invalid multi-factor authentication code");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
                
                // MFA verification successful - clear rate limit
                rateLimitingService.clearMfaVerificationAttempts(user.getEmail());
                
                System.out.println("✅ MFA verified successfully for: " + loginRequest.getEmail() + " via " + methodUsed);
                activityLogService.logActivity(
                    user.getId().toString(),
                    user.getUsername(),
                    "MFA_VERIFIED",
                    "AUTHENTICATION",
                    "MFA code verified successfully via " + methodUsed + ": " + loginRequest.getEmail()
                );
            }
            
            // Automatic tenant resolution and JWT generation (no tenantId in request required)
            Object tenantResolutionResult = resolveTenantForUser(user);
            
            // Check if tenant selection is required (multiple memberships, no default)
            if (tenantResolutionResult instanceof com.pos.inventsight.dto.TenantSelectionResponse) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(tenantResolutionResult);
            }
            
            // Check for error messages or valid tenant UUID
            String tenantId;
            if (tenantResolutionResult instanceof String) {
                String resultString = (String) tenantResolutionResult;
                
                // Try to parse as UUID - if successful, it's a tenant ID
                try {
                    java.util.UUID.fromString(resultString);
                    // Valid UUID - this is the tenant ID
                    tenantId = resultString;
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID - treat as error message
                    if (resultString.startsWith("NO_TENANT_MEMBERSHIP")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new AuthResponse(resultString));
                    } else if (resultString.startsWith("TENANT_CREATION_FAILED")) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new AuthResponse(resultString));
                    }
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse(resultString));
                }
            } else {
                // Unexpected type - should not happen
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Unexpected tenant resolution result"));
            }
            String jwt = jwtUtils.generateJwtToken(user, tenantId);
            
            // Update last login
            userService.updateLastLogin(user.getId());
            
            // Log authentication activity
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("loginTime", LocalDateTime.now());
            metadata.put("userAgent", "InventSight API");
            metadata.put("ipAddress", "system");
            if (loginRequest.getTenantId() != null) {
                metadata.put("tenantId", loginRequest.getTenantId());
            }
            
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
            
            // Check email verification status after successful authentication
            // This provides better UX for existing users while maintaining security
            // by not exposing which emails are registered (credentials are validated first)
            if (user.getEmailVerified() == null || !user.getEmailVerified()) {
                System.out.println("❌ Login blocked - email not verified for: " + loginRequest.getEmail());
                
                // Log failed authentication attempt due to unverified email
                activityLogService.logActivity(
                    user.getId().toString(),
                    user.getUsername(),
                    "USER_LOGIN_V2_EMAIL_UNVERIFIED",
                    "AUTHENTICATION",
                    "Login attempt with unverified email (structured): " + loginRequest.getEmail()
                );
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new StructuredAuthResponse("Email not verified. Please verify your email before logging in.", false));
            }
            
            // Check MFA status and validate code if enabled
            boolean mfaEnabled = mfaService.isMfaEnabled(user);
            if (mfaEnabled) {
                // Get user's preferred MFA delivery method
                com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod preferredMethod = 
                    mfaService.getPreferredDeliveryMethod(user);
                
                // Check if any MFA code is provided
                boolean hasTotpCode = loginRequest.getTotpCode() != null;
                boolean hasOtpCode = loginRequest.getOtpCode() != null && !loginRequest.getOtpCode().isEmpty();
                
                if (!hasTotpCode && !hasOtpCode) {
                    System.out.println("❌ Login blocked - MFA code required for: " + loginRequest.getEmail());
                    
                    // Log MFA required event
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_REQUIRED",
                        "AUTHENTICATION",
                        "Login attempt without MFA code (structured): " + loginRequest.getEmail()
                    );
                    
                    StructuredAuthResponse errorResponse = new StructuredAuthResponse(
                        "Multi-factor authentication code is required. Preferred method: " + preferredMethod, false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
                
                // Check rate limiting for MFA login attempts
                if (!rateLimitingService.isMfaVerificationAllowed(user.getEmail())) {
                    System.out.println("❌ Login blocked - MFA rate limit exceeded for: " + loginRequest.getEmail());
                    
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_RATE_LIMITED",
                        "AUTHENTICATION",
                        "MFA login rate limit exceeded (structured): " + loginRequest.getEmail()
                    );
                    
                    StructuredAuthResponse errorResponse = new StructuredAuthResponse(
                        "Too many MFA verification attempts. Please try again later.", false);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
                }
                
                // Record MFA verification attempt
                rateLimitingService.recordMfaVerificationAttempt(user.getEmail());
                
                boolean codeValid = false;
                String methodUsed = null;
                
                // Verify based on the code type provided
                if (hasOtpCode) {
                    // OTP code provided - verify as email/SMS OTP
                    com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod deliveryMethod = 
                        preferredMethod == com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.SMS
                            ? com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.SMS
                            : com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.EMAIL;
                    
                    codeValid = mfaService.verifyOtpCode(user, loginRequest.getOtpCode(), deliveryMethod);
                    methodUsed = deliveryMethod.toString();
                } else if (hasTotpCode) {
                    // TOTP code provided - verify as TOTP
                    codeValid = mfaService.verifyCode(user, loginRequest.getTotpCode());
                    methodUsed = "TOTP";
                }
                
                if (!codeValid) {
                    System.out.println("❌ Login blocked - invalid MFA code for: " + loginRequest.getEmail());
                    
                    // Log MFA validation failure
                    activityLogService.logActivity(
                        user.getId().toString(),
                        user.getUsername(),
                        "MFA_FAILED",
                        "AUTHENTICATION",
                        "Login attempt with invalid MFA code (structured): " + loginRequest.getEmail()
                    );
                    
                    StructuredAuthResponse errorResponse = new StructuredAuthResponse(
                        "Invalid multi-factor authentication code", false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
                
                // MFA verification successful - clear rate limit
                rateLimitingService.clearMfaVerificationAttempts(user.getEmail());
                
                // MFA verification successful
                System.out.println("✅ MFA verified successfully for: " + loginRequest.getEmail() + " via " + methodUsed);
                activityLogService.logActivity(
                    user.getId().toString(),
                    user.getUsername(),
                    "MFA_VERIFIED",
                    "AUTHENTICATION",
                    "MFA code verified successfully via " + methodUsed + " (structured): " + loginRequest.getEmail()
                );
            }
            
            // Automatic tenant resolution and JWT generation (no tenantId in request required)
            Object tenantResolutionResult = resolveTenantForUser(user);
            
            // Check if tenant selection is required (multiple memberships, no default)
            if (tenantResolutionResult instanceof com.pos.inventsight.dto.TenantSelectionResponse) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(tenantResolutionResult);
            }
            
            // Check for error messages or valid tenant UUID
            String tenantId;
            if (tenantResolutionResult instanceof String) {
                String resultString = (String) tenantResolutionResult;
                
                // Try to parse as UUID - if successful, it's a tenant ID
                try {
                    java.util.UUID.fromString(resultString);
                    // Valid UUID - this is the tenant ID
                    tenantId = resultString;
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID - treat as error message
                    if (resultString.startsWith("NO_TENANT_MEMBERSHIP")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new StructuredAuthResponse(resultString, false));
                    }
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new StructuredAuthResponse(resultString, false));
                }
            } else {
                // Unexpected type - should not happen
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StructuredAuthResponse("Unexpected tenant resolution result", false));
            }
            String accessToken = jwtUtils.generateJwtToken(user, tenantId);
            
            String refreshToken = jwtUtils.generateRefreshToken(user);
            
            // Update last login
            userService.updateLastLogin(user.getId());
            
            // Log authentication activity
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("loginTime", LocalDateTime.now());
            metadata.put("userAgent", "InventSight API");
            metadata.put("ipAddress", "system");
            if (loginRequest.getTenantId() != null) {
                metadata.put("tenantId", loginRequest.getTenantId());
            }
            
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
    
    /**
     * Authentication-specific user registration endpoint
     * 
     * Handles POST requests to /auth/register (which becomes /api/auth/register due to context path).
     * This is the primary authentication endpoint for user registration with full security features
     * including rate limiting, password validation, and immediate JWT token generation.
     * 
     * @param registerRequest JSON request body containing user registration data (username, email, password, firstName, lastName)
     * @param request HTTP request for extracting client information for rate limiting
     * @return ResponseEntity with authentication response or error details
     */
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
            user.setRole(UserRole.MANAGER); // Founder gets General Manager role
            
            User savedUser = userService.createUser(user);
            
            // Auto-send verification email
            sendVerificationEmailAfterRegistration(savedUser);
            
            // Generate tenant-bound JWT token for immediate login with default tenant
            // User now has defaultTenantId set automatically by createUser
            String jwt = jwtUtils.generateJwtToken(savedUser, savedUser.getDefaultTenantId().toString());
            
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
    
    /**
     * User signup endpoint (frontend compatibility alias for registration)
     * 
     * Handles POST requests to /auth/signup providing an alternative endpoint name
     * for frontend applications that prefer "signup" terminology over "register".
     * Provides the same functionality as the main registration endpoint.
     * 
     * @param signupRequest JSON request body containing user registration data
     * @param request HTTP request for extracting client information
     * @return ResponseEntity with structured authentication response or error details
     */
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
            user.setRole(UserRole.MANAGER); // Founder gets General Manager role
            
            User savedUser = userService.createUser(user);
            
            // Auto-send verification email
            sendVerificationEmailAfterRegistration(savedUser);
            
            // Generate tenant-bound JWT tokens with default tenant
            // User now has defaultTenantId set automatically by createUser
            String accessToken = jwtUtils.generateJwtToken(savedUser, savedUser.getDefaultTenantId().toString());
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
    
    /**
     * Automatically resolve tenant for user at login (no tenantId in request required)
     * Returns: UUID string if resolved, TenantSelectionResponse if selection needed, String error otherwise
     */
    private Object resolveTenantForUser(User user) {
        // Step 1: Check if user has a default tenant set
        if (user.getDefaultTenantId() != null) {
            // Verify the default tenant is still valid and user still has active membership
            if (companyRepository.existsById(user.getDefaultTenantId())) {
                java.util.List<com.pos.inventsight.model.sql.CompanyStoreUser> memberships = 
                    companyStoreUserRepository.findByUserAndIsActiveTrue(user);
                
                boolean hasDefaultMembership = memberships.stream()
                    .anyMatch(m -> m.getCompany().getId().equals(user.getDefaultTenantId()));
                
                if (hasDefaultMembership) {
                    System.out.println("✅ Using default tenant for user: " + user.getDefaultTenantId());
                    return user.getDefaultTenantId().toString();
                } else {
                    System.out.println("⚠️ User's default tenant is no longer valid, resolving from memberships");
                }
            }
        }
        
        // Step 2: Fetch active memberships
        java.util.List<com.pos.inventsight.model.sql.CompanyStoreUser> memberships = 
            companyStoreUserRepository.findByUserAndIsActiveTrue(user);
        
        if (memberships.isEmpty()) {
            System.out.println("⚠️ User has no active tenant memberships - creating default company");
            // Create a default company for the user
            try {
                com.pos.inventsight.model.sql.Company defaultCompany = createDefaultCompanyForUser(user);
                user.setDefaultTenantId(defaultCompany.getId());
                userService.saveUser(user);
                System.out.println("✅ Created default company and set as default tenant: " + defaultCompany.getId());
                return defaultCompany.getId().toString();
            } catch (Exception e) {
                System.out.println("❌ Failed to create default company for user: " + e.getMessage());
                return "TENANT_CREATION_FAILED: Unable to create default tenant. Please contact your administrator. Error: " + e.getMessage();
            }
        }
        
        // Step 3: If exactly one membership, auto-set as default and use it
        if (memberships.size() == 1) {
            java.util.UUID tenantId = memberships.get(0).getCompany().getId();
            user.setDefaultTenantId(tenantId);
            userService.saveUser(user);
            System.out.println("✅ Auto-set single membership as default tenant: " + tenantId);
            return tenantId.toString();
        }
        
        // Step 4: Multiple memberships and no valid default - return selection required
        System.out.println("⚠️ Multiple memberships found, tenant selection required");
        java.util.List<com.pos.inventsight.dto.TenantSelectionResponse.MemberCompany> companies = 
            new java.util.ArrayList<>();
        
        for (com.pos.inventsight.model.sql.CompanyStoreUser membership : memberships) {
            companies.add(new com.pos.inventsight.dto.TenantSelectionResponse.MemberCompany(
                membership.getCompany().getId().toString(),
                membership.getCompany().getName(),
                membership.getRole().name(),
                membership.getIsActive()
            ));
        }
        
        return new com.pos.inventsight.dto.TenantSelectionResponse(
            "TENANT_SELECTION_REQUIRED",
            "Multiple tenant memberships found. Please select a tenant to continue.",
            companies
        );
    }
    
    /**
     * Create a default company for a user who has no company memberships
     */
    private com.pos.inventsight.model.sql.Company createDefaultCompanyForUser(User user) {
        // Create company with user's information
        com.pos.inventsight.model.sql.Company company = new com.pos.inventsight.model.sql.Company();
        company.setName(user.getFullName() + "'s Company");
        company.setEmail(user.getEmail());
        company.setIsActive(true);
        company.setCreatedBy(user.getUsername());
        company.setUpdatedBy(user.getUsername());
        company.setCreatedAt(java.time.LocalDateTime.now());
        company.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Save the company
        company = companyRepository.save(company);
        
        // Create company-user membership with FOUNDER role
        com.pos.inventsight.model.sql.CompanyStoreUser membership = 
            new com.pos.inventsight.model.sql.CompanyStoreUser(
                company, 
                user, 
                com.pos.inventsight.model.sql.CompanyRole.FOUNDER,
                "SYSTEM"
            );
        membership.setIsActive(true);
        companyStoreUserRepository.save(membership);
        
        // Log activity
        activityLogService.logActivity(
            user.getId().toString(),
            user.getUsername(),
            "DEFAULT_COMPANY_CREATED",
            "AUTHENTICATION",
            "Default company created for user: " + user.getEmail() + ", Company: " + company.getName()
        );
        
        return company;
    }
    
    /**
     * Send OTP code for login flow (public endpoint - no authentication required)
     * This endpoint allows users to receive OTP codes during the login process before they have a valid JWT token.
     */
    @PostMapping("/mfa/send-login-otp")
    public ResponseEntity<?> sendLoginOtp(@Valid @RequestBody com.pos.inventsight.dto.MfaSendOtpRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            System.out.println("📨 Sending login OTP to: " + request.getEmail());
            
            // Validate that email is provided for EMAIL delivery method
            if (request.getDeliveryMethod() == com.pos.inventsight.dto.MfaSendOtpRequest.DeliveryMethod.EMAIL) {
                if (request.getEmail() == null || request.getEmail().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Email address is required for EMAIL delivery method");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // Validate that phone number is provided for SMS delivery method
            if (request.getDeliveryMethod() == com.pos.inventsight.dto.MfaSendOtpRequest.DeliveryMethod.SMS) {
                if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Phone number is required for SMS delivery method");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // Find user by email or phone number
            User user = null;
            if (request.getDeliveryMethod() == com.pos.inventsight.dto.MfaSendOtpRequest.DeliveryMethod.EMAIL) {
                user = userService.getUserByEmail(request.getEmail());
            } else {
                // For SMS, we need to find user by phone number
                // This might require a new method in UserService if not already available
                throw new IllegalStateException("SMS delivery method not yet implemented for login OTP");
            }
            
            if (user == null) {
                // Don't reveal if user exists - return generic success message for security
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "If an account exists with that information, an OTP code will be sent");
                response.put("deliveryMethod", request.getDeliveryMethod());
                return ResponseEntity.ok(response);
            }
            
            // Check if MFA is enabled for this user
            if (!mfaService.isMfaEnabled(user)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "MFA is not enabled for this account");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert DTO delivery method to entity enum
            com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod deliveryMethod = 
                request.getDeliveryMethod() == com.pos.inventsight.dto.MfaSendOtpRequest.DeliveryMethod.EMAIL
                    ? com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.EMAIL
                    : com.pos.inventsight.model.sql.MfaSecret.DeliveryMethod.SMS;
            
            // Get IP address
            String ipAddress = httpRequest.getRemoteAddr();
            
            // Send OTP code
            mfaService.sendOtpCode(user, deliveryMethod, ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP code sent successfully via " + request.getDeliveryMethod());
            response.put("deliveryMethod", request.getDeliveryMethod());
            
            return ResponseEntity.ok(response);
            
        } catch (com.pos.inventsight.exception.ResourceNotFoundException e) {
            // Don't reveal if user exists - return generic success message for security
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If an account exists with that information, an OTP code will be sent");
            response.put("deliveryMethod", request.getDeliveryMethod());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.err.println("❌ Error sending login OTP: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error sending OTP code. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    
    // POST /auth/tenant-select - Select tenant for users with multiple memberships
    @PostMapping("/tenant-select")
    public ResponseEntity<?> selectTenant(@Valid @RequestBody com.pos.inventsight.dto.TenantSelectRequest tenantSelectRequest,
                                         HttpServletRequest request) {
        System.out.println("🏢 InventSight - Processing tenant selection request");
        System.out.println("📅 Current DateTime (UTC): 2025-11-02");
        
        try {
            // Get authenticated user from request
            String headerAuth = request.getHeader("Authorization");
            
            if (headerAuth == null || !headerAuth.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Authorization required"));
            }
            
            String jwt = headerAuth.substring(7);
            
            if (!jwtUtils.validateJwtToken(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid or expired token"));
            }
            
            String username = jwtUtils.getUsernameFromJwtToken(jwt);
            User user = userService.getUserByEmail(username);
            
            // Validate tenant ID
            java.util.UUID tenantUuid;
            try {
                tenantUuid = java.util.UUID.fromString(tenantSelectRequest.getTenantId());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Invalid tenant ID format. Must be a valid UUID."));
            }
            
            // Verify company exists
            if (!companyRepository.existsById(tenantUuid)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("Company not found for the specified tenant ID."));
            }
            
            // Verify user has active membership in the company
            java.util.List<com.pos.inventsight.model.sql.CompanyStoreUser> memberships = 
                companyStoreUserRepository.findByUserAndIsActiveTrue(user);
            
            boolean hasMembership = memberships.stream()
                .anyMatch(m -> m.getCompany().getId().equals(tenantUuid) && m.getIsActive());
            
            if (!hasMembership) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse("Access denied: user is not a member of the specified company."));
            }
            
            // Set as default tenant and persist
            user.setDefaultTenantId(tenantUuid);
            userService.saveUser(user);
            
            // Generate tenant-bound JWT
            String newJwt = jwtUtils.generateJwtToken(user, tenantUuid.toString());
            
            // Log activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "TENANT_SELECTED",
                "AUTHENTICATION",
                "User selected default tenant: " + tenantUuid
            );
            
            // Create response
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
            
            System.out.println("✅ Tenant selected and set as default: " + tenantUuid);
            return ResponseEntity.ok(authResponse);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthResponse("User not found"));
        } catch (Exception e) {
            System.out.println("❌ Tenant selection error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Tenant selection service temporarily unavailable"));
        }
    }
    
    // POST /auth/invite/accept - Accept invite and set default tenant
    @PostMapping("/invite/accept")
    public ResponseEntity<?> acceptInvite(@Valid @RequestBody com.pos.inventsight.dto.InviteAcceptRequest inviteRequest) {
        System.out.println("📨 InventSight - Processing invite acceptance");
        System.out.println("📅 Current DateTime (UTC): 2025-11-02");
        
        try {
            // TODO: Implement invite token validation and resolution
            // Returning 503 Service Unavailable until invite service is fully integrated
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "3600") // Suggest retry after 1 hour
                .body(new AuthResponse("Invite acceptance service is under development. Please contact support for manual invite processing."));
            
        } catch (Exception e) {
            System.out.println("❌ Invite acceptance error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Invite acceptance service temporarily unavailable"));
        }
    }
    
    // POST /auth/refresh - Token refresh endpoint
    @PostMapping("/refresh-sample")
    public ResponseEntity<?> refreshTokenSample(HttpServletRequest request) {
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
    public ResponseEntity<?> logoutUser(@RequestBody(required = false) RefreshTokenRequest request, 
                                       HttpServletRequest httpRequest) {
        System.out.println("🚪 InventSight - Processing logout request");
        System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
        System.out.println("👤 Current User: WinKyaw");
        
        try {
            String headerAuth = httpRequest.getHeader("Authorization");
            
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String jwt = headerAuth.substring(7);
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    User user = userService.getUserByEmail(username);
                    
                    // Revoke refresh token if provided
                    if (refreshTokenService != null && request != null && 
                        request.getRefreshToken() != null) {
                        try {
                            refreshTokenService.revokeToken(request.getRefreshToken());
                            System.out.println("✅ Refresh token revoked");
                        } catch (Exception e) {
                            System.out.println("⚠️ Failed to revoke refresh token: " + e.getMessage());
                        }
                    }
                    
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
    
    // POST /auth/refresh - Refresh access token using refresh token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                         HttpServletRequest httpRequest) {
        System.out.println("🔄 InventSight - Processing token refresh request");
        System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
        
        try {
            if (refreshTokenService == null) {
                System.out.println("⚠️ Refresh token service not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthResponse("Token refresh not available"));
            }
            
            // Validate refresh token
            RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
            User user = refreshToken.getUser();
            
            System.out.println("✅ Refresh token valid for user: " + user.getEmail());
            
            // Resolve tenant for user
            Object tenantResolutionResult = resolveTenantForUser(user);
            
            // Check if tenant selection is required
            if (tenantResolutionResult instanceof com.pos.inventsight.dto.TenantSelectionResponse) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(tenantResolutionResult);
            }
            
            // Extract tenant ID
            String tenantId;
            if (tenantResolutionResult instanceof String) {
                String resultString = (String) tenantResolutionResult;
                try {
                    java.util.UUID.fromString(resultString);
                    tenantId = resultString;
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse("Invalid tenant resolution: " + resultString));
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Unexpected tenant resolution result"));
            }
            
            // Generate new access token
            String newAccessToken = jwtUtils.generateJwtToken(user, tenantId);
            
            // Log token refresh activity
            activityLogService.logActivity(
                user.getId().toString(),
                user.getUsername(),
                "TOKEN_REFRESH",
                "AUTHENTICATION",
                "Access token refreshed for user: " + user.getEmail()
            );
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpirationMs / 1000); // seconds
            response.put("refreshToken", request.getRefreshToken()); // return same refresh token
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            System.out.println("✅ New access token generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.out.println("❌ Token refresh failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("Invalid or expired refresh token: " + e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Token refresh error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Token refresh failed: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to send verification email after user registration
     * Handles email sending errors gracefully without failing registration
     */
    private void sendVerificationEmailAfterRegistration(User user) {
        try {
            String verificationToken = emailVerificationService.generateVerificationToken(user.getEmail());
            emailVerificationService.sendVerificationEmail(user.getEmail(), verificationToken);
            logger.info("✅ Verification email sent to: {}", user.getEmail());
        } catch (Exception emailError) {
            logger.warn("⚠️ Warning: Failed to send verification email to {}: {}", 
                user.getEmail(), emailError.getMessage());
            // Don't fail registration if email fails
        }
    }
}