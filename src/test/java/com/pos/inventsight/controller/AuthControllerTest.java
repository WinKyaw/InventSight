package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.config.JwtUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ActivityLogService activityLogService;
    
    @MockBean
    private com.pos.inventsight.service.MfaService mfaService;
    
    @MockBean
    private com.pos.inventsight.repository.sql.CompanyRepository companyRepository;
    
    @MockBean
    private com.pos.inventsight.repository.sql.CompanyStoreUserRepository companyStoreUserRepository;
    
    @MockBean
    private com.pos.inventsight.service.EmailVerificationService emailVerificationService;
    
    @MockBean
    private com.pos.inventsight.service.PasswordValidationService passwordValidationService;
    
    @MockBean
    private com.pos.inventsight.service.RateLimitingService rateLimitingService;
    
    @MockBean
    private com.pos.inventsight.service.IdempotencyService idempotencyService;
    
    @MockBean
    private com.pos.inventsight.service.AuditService auditService;
    
    @MockBean
    private com.pos.inventsight.config.AuthEntryPointJwt authEntryPointJwt;
    
    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    @MockBean
    private com.pos.inventsight.tenant.CompanyTenantFilter companyTenantFilter;
    
    @MockBean
    private com.pos.inventsight.filter.RateLimitingFilter rateLimitingFilter;
    
    @MockBean
    private com.pos.inventsight.filter.IdempotencyKeyFilter idempotencyKeyFilter;
    
    @MockBean
    private com.pos.inventsight.repository.sql.MfaSecretRepository mfaSecretRepository;
    
    @MockBean
    private com.pos.inventsight.repository.sql.MfaBackupCodeRepository mfaBackupCodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLoginSuccess() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@inventsight.com");
        mockUser.setUsername("testuser");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@inventsight.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Verify interactions
        verify(userService).updateLastLogin(any(UUID.class));
        verify(activityLogService).logActivityWithMetadata(eq("1"), eq("testuser"), 
                eq("USER_LOGIN"), eq("AUTHENTICATION"), anyString(), any());
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@inventsight.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("newuser@inventsight.com");
        savedUser.setUsername("newuser");
        savedUser.setFirstName("New");
        savedUser.setLastName("User");
        savedUser.setRole(UserRole.USER);

        when(userService.createUser(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token-new");

        // When & Then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-new"))
                .andExpect(jsonPath("$.email").value("newuser@inventsight.com"))
                .andExpect(jsonPath("$.username").value("newuser"));

        // Verify interactions
        verify(userService).createUser(any(User.class));
        verify(activityLogService).logActivity(eq("2"), eq("newuser"), 
                eq("USER_REGISTERED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLoginInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("invalid@inventsight.com");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // Verify failed login was logged
        verify(activityLogService).logActivity(isNull(), eq("WinKyaw"), 
                eq("USER_LOGIN_FAILED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLoginWithUnverifiedEmail() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unverified@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("unverified@inventsight.com");
        mockUser.setUsername("unverifieduser");
        mockUser.setFirstName("Unverified");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(false); // Email not verified

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email not verified. Please verify your email before logging in."));

        // Verify email verification failure was logged
        verify(activityLogService).logActivity(eq("1"), eq("unverifieduser"), 
                eq("USER_LOGIN_EMAIL_UNVERIFIED"), eq("AUTHENTICATION"), anyString());
        
        // Verify that last login was NOT updated for unverified users
        verify(userService, never()).updateLastLogin(any(UUID.class));
    }

    @Test
    public void testLoginV2WithUnverifiedEmail() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unverified@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("unverified@inventsight.com");
        mockUser.setUsername("unverifieduser");
        mockUser.setFirstName("Unverified");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(false); // Email not verified

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // When & Then
        mockMvc.perform(post("/auth/login/v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email not verified. Please verify your email before logging in."))
                .andExpect(jsonPath("$.success").value(false));

        // Verify email verification failure was logged
        verify(activityLogService).logActivity(eq("1"), eq("unverifieduser"), 
                eq("USER_LOGIN_V2_EMAIL_UNVERIFIED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@inventsight.com");
        mockUser.setUsername("testuser");

        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken("valid-token")).thenReturn("test@inventsight.com");
        when(userService.getUserByEmail("test@inventsight.com")).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully"));

        // Verify logout was logged
        verify(activityLogService).logActivity(eq("1"), eq("testuser"), 
                eq("USER_LOGOUT"), eq("AUTHENTICATION"), anyString());
    }
    
    @Test
    public void testLoginWithMfaRequiredButCodeMissing() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("mfa@inventsight.com");
        loginRequest.setPassword("password123");
        // totpCode is null

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("mfa@inventsight.com");
        mockUser.setUsername("mfauser");
        mockUser.setFirstName("MFA");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Multi-factor authentication code is required"));

        // Verify MFA required was logged
        verify(activityLogService).logActivity(eq("1"), eq("mfauser"), 
                eq("MFA_REQUIRED"), eq("AUTHENTICATION"), anyString());
        
        // Verify that last login was NOT updated
        verify(userService, never()).updateLastLogin(any(UUID.class));
    }
    
    @Test
    public void testLoginWithMfaInvalidCode() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("mfa@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTotpCode(123456); // Invalid code

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("mfa@inventsight.com");
        mockUser.setUsername("mfauser");
        mockUser.setFirstName("MFA");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(true);
        when(mfaService.verifyCode(mockUser, 123456)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("MFA_INVALID_CODE"))
                .andExpect(jsonPath("$.message").value("Invalid multi-factor authentication code"));

        // Verify MFA failure was logged
        verify(activityLogService).logActivity(eq("1"), eq("mfauser"), 
                eq("MFA_FAILED"), eq("AUTHENTICATION"), anyString());
        
        // Verify that last login was NOT updated
        verify(userService, never()).updateLastLogin(any(UUID.class));
    }
    
    @Test
    public void testLoginWithMfaSuccessNoTenant() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("mfa@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTotpCode(123456); // Valid code

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("mfa@inventsight.com");
        mockUser.setUsername("mfauser");
        mockUser.setFirstName("MFA");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(true);
        when(mfaService.verifyCode(mockUser, 123456)).thenReturn(true);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token-mfa");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-mfa"))
                .andExpect(jsonPath("$.email").value("mfa@inventsight.com"));

        // Verify MFA verification success was logged
        verify(activityLogService).logActivity(eq("1"), eq("mfauser"), 
                eq("MFA_VERIFIED"), eq("AUTHENTICATION"), anyString());
        
        // Verify user login was logged
        verify(activityLogService).logActivityWithMetadata(eq("1"), eq("mfauser"), 
                eq("USER_LOGIN"), eq("AUTHENTICATION"), anyString(), any());
        
        // Verify last login was updated
        verify(userService).updateLastLogin(any(UUID.class));
    }
    
    @Test
    public void testLoginWithTenantIdSuccess() throws Exception {
        // Given
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tenant@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId(tenantId);

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("tenant@inventsight.com");
        mockUser.setUsername("tenantuser");
        mockUser.setFirstName("Tenant");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(companyRepository.existsById(any())).thenReturn(true);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
                .thenReturn(java.util.Arrays.asList(createMockCompanyStoreUser(mockUser, tenantId)));
        when(jwtUtils.generateJwtToken(any(User.class), eq(tenantId)))
                .thenReturn("mock-jwt-token-tenant");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-tenant"));

        // Verify tenant-bound JWT was generated
        verify(jwtUtils).generateJwtToken(any(User.class), eq(tenantId));
    }
    
    @Test
    public void testLoginWithInvalidTenantId() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tenant@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId("invalid-uuid");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("tenant@inventsight.com");
        mockUser.setUsername("tenantuser");
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid tenant ID format. Must be a valid UUID."));
    }
    
    @Test
    public void testLoginWithTenantNotFound() throws Exception {
        // Given
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tenant@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId(tenantId);

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("tenant@inventsight.com");
        mockUser.setUsername("tenantuser");
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(companyRepository.existsById(any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Company not found for the specified tenant ID."));
    }
    
    @Test
    public void testLoginWithNoTenantMembership() throws Exception {
        // Given
        String tenantId = "123e4567-e89b-12d3-a456-426614174000";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tenant@inventsight.com");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId(tenantId);

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("tenant@inventsight.com");
        mockUser.setUsername("tenantuser");
        mockUser.setEmailVerified(true);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(companyRepository.existsById(any())).thenReturn(true);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
                .thenReturn(java.util.Collections.emptyList()); // No membership

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not a member of the specified company."));
    }
    
    @Test
    public void testLoginWithDefaultTenantReturnsSuccessNotError() throws Exception {
        // This test validates the fix for the bug where tenant UUIDs were being treated as error messages
        // Given
        String validTenantUuid = "24ad4dc1-806b-4736-8213-104f4190258b";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("windaybunce@gmail.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("windaybunce@gmail.com");
        mockUser.setUsername("Jennie1");
        mockUser.setFirstName("JJ");
        mockUser.setLastName("Win");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);
        mockUser.setDefaultTenantId(java.util.UUID.fromString(validTenantUuid));

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(companyRepository.existsById(java.util.UUID.fromString(validTenantUuid))).thenReturn(true);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
                .thenReturn(java.util.Arrays.asList(createMockCompanyStoreUser(mockUser, validTenantUuid)));
        when(jwtUtils.generateJwtToken(any(User.class), eq(validTenantUuid)))
                .thenReturn("mock-jwt-token-with-tenant");

        // When & Then
        // The fix ensures that a valid UUID string is NOT treated as an error
        // Before fix: would return 400 BAD_REQUEST with tenant UUID as error message
        // After fix: should return 200 OK with JWT token
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-with-tenant"))
                .andExpect(jsonPath("$.email").value("windaybunce@gmail.com"))
                .andExpect(jsonPath("$.username").value("Jennie1"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Verify tenant-bound JWT was generated with the correct UUID
        verify(jwtUtils).generateJwtToken(any(User.class), eq(validTenantUuid));
        verify(userService).updateLastLogin(any(UUID.class));
    }
    
    @Test
    public void testLoginWithNoTenantMembershipReturnsProperError() throws Exception {
        // This test ensures error messages are still properly handled
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("notenant@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("notenant@inventsight.com");
        mockUser.setUsername("notenantuser");
        mockUser.setFirstName("No");
        mockUser.setLastName("Tenant");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(true);
        mockUser.setDefaultTenantId(null); // No default tenant

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mfaService.isMfaEnabled(mockUser)).thenReturn(false);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(mockUser))
                .thenReturn(java.util.Collections.emptyList()); // No memberships

        // When & Then
        // Should return 403 FORBIDDEN with proper error message
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NO_TENANT_MEMBERSHIP")));
    }
    
    // Helper method to create mock CompanyStoreUser
    private com.pos.inventsight.model.sql.CompanyStoreUser createMockCompanyStoreUser(User user, String companyId) {
        com.pos.inventsight.model.sql.CompanyStoreUser csu = mock(com.pos.inventsight.model.sql.CompanyStoreUser.class);
        com.pos.inventsight.model.sql.Company company = mock(com.pos.inventsight.model.sql.Company.class);
        when(company.getId()).thenReturn(java.util.UUID.fromString(companyId));
        when(csu.getCompany()).thenReturn(company);
        when(csu.getIsActive()).thenReturn(true);
        when(csu.getUser()).thenReturn(user);
        return csu;
    }
}