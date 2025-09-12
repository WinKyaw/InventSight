package com.pos.inventsight.controller;

import com.pos.inventsight.dto.AuthResponse;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.PasswordValidationService;
import com.pos.inventsight.service.RateLimitingService;
import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.exception.DuplicateResourceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Registration Controller
 * 
 * Handles user registration requests for the main API endpoints.
 * This controller provides a direct /register endpoint that complements 
 * the authentication-specific /auth/register endpoint.
 */
@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RegistrationController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private PasswordValidationService passwordValidationService;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Value("${inventsight.security.jwt.expiration:86400000}")
    private Long jwtExpirationMs;
    
    /**
     * Main API registration endpoint
     * 
     * Handles POST requests to /register (which becomes /api/register due to context path).
     * Accepts user registration data and creates a new user account with immediate authentication.
     * 
     * @param registerRequest JSON request body containing user registration data
     * @param request HTTP request for extracting client information
     * @return ResponseEntity with authentication response or error details
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, 
                                         HttpServletRequest request) {
        System.out.println("🔐 InventSight - Processing API registration request for: " + registerRequest.getEmail());
        System.out.println("📅 Current DateTime (UTC): 2025-08-27 10:27:11");
        System.out.println("👤 Current User: WinKyaw");
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check rate limiting for registration attempts
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
                
                System.out.println("❌ API Registration rate limited for: " + registerRequest.getEmail());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
            
            // Record registration attempt for rate limiting
            rateLimitingService.recordRegistrationAttempt(clientIp, registerRequest.getEmail());
            
            // Validate password strength according to security requirements
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
                
                System.out.println("❌ API Registration failed - weak password for: " + registerRequest.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Create new user entity with registration data
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setRole(UserRole.USER); // Default role for new registrations
            
            // Save user to database (includes password hashing and validation)
            User savedUser = userService.createUser(user);
            
            // Generate JWT token for immediate authentication after registration
            String jwt = jwtUtils.generateJwtToken(savedUser);
            
            // Log successful registration activity for audit trail
            activityLogService.logActivity(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                "USER_REGISTERED_API",
                "AUTHENTICATION",
                "New user registered via API endpoint: " + savedUser.getEmail()
            );
            
            // Create successful authentication response
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
            
            System.out.println("✅ User registered successfully via API: " + savedUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (DuplicateResourceException e) {
            // Handle duplicate email or username
            System.out.println("❌ API Registration failed - duplicate resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthResponse(e.getMessage()));
                
        } catch (Exception e) {
            // Handle unexpected errors
            System.out.println("❌ API Registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("Registration service temporarily unavailable"));
        }
    }
    
    /**
     * Helper method to extract client IP address from HTTP request
     * 
     * Checks various headers that may contain the real client IP address
     * when the application is behind proxies or load balancers.
     * 
     * @param request HTTP servlet request
     * @return String containing the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For may contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}