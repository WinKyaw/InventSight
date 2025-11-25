package com.pos.inventsight.controller;

import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.MfaService;
import com.pos.inventsight.config.JwtUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for JWT token tenant_id fix - ensures JWT tokens always contain tenant_id claim
 */
@SpringBootTest
@ActiveProfiles("test")
public class AuthControllerTenantIdTest {

    @Autowired
    private AuthController authController;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ActivityLogService activityLogService;

    @MockBean
    private MfaService mfaService;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private CompanyStoreUserRepository companyStoreUserRepository;

    private User testUser;
    private Company testCompany;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());  // User uses Long id
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
        testUser.setEmailVerified(true);
        testUser.setPassword("encoded_password");

        // Create test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test User's Company");
        testCompany.setEmail("test@example.com");
        testCompany.setIsActive(true);

        // Mock authentication
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(testUser);
    }

    /**
     * Test that JWT contains tenant_id when user has no memberships (creates default company)
     */
    @Test
    void testLoginCreatesDefaultCompanyWhenNoMemberships() throws Exception {
        // Given: User with no company memberships
        testUser.setDefaultTenantId(null);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(new ArrayList<>());
        
        // Mock company creation
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        
        // Mock JWT generation with tenant_id
        String expectedJwt = "jwt_token_with_tenant_id";
        when(jwtUtils.generateJwtToken(eq(testUser), eq(testCompany.getId().toString())))
            .thenReturn(expectedJwt);
        
        // Mock MFA check
        when(mfaService.isMfaEnabled(testUser)).thenReturn(false);
        
        // When: User logs in
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Then: Default company should be created
        verify(companyRepository, times(1)).save(any(Company.class));
        
        // And: Company-user membership should be created with FOUNDER role
        verify(companyStoreUserRepository, times(1)).save(any(CompanyStoreUser.class));
        
        // And: User's defaultTenantId should be updated
        verify(userService, times(1)).saveUser(testUser);
        
        // And: JWT should be generated with tenant_id
        verify(jwtUtils, times(1))
            .generateJwtToken(eq(testUser), eq(testCompany.getId().toString()));
        
        // And: Response should be successful
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * Test that JWT contains tenant_id when user has existing default tenant
     */
    @Test
    void testLoginUsesExistingDefaultTenant() throws Exception {
        // Given: User with existing default tenant
        testUser.setDefaultTenantId(testCompany.getId());
        
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication);
        when(companyRepository.existsById(testCompany.getId())).thenReturn(true);
        
        // Create membership
        CompanyStoreUser membership = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER);
        membership.setIsActive(true);
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(memberships);
        
        // Mock JWT generation
        String expectedJwt = "jwt_token_with_tenant_id";
        when(jwtUtils.generateJwtToken(eq(testUser), eq(testCompany.getId().toString())))
            .thenReturn(expectedJwt);
        
        // Mock MFA check
        when(mfaService.isMfaEnabled(testUser)).thenReturn(false);
        
        // When: User logs in
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Then: No new company should be created
        verify(companyRepository, never()).save(any(Company.class));
        
        // And: JWT should be generated with existing tenant_id
        verify(jwtUtils, times(1))
            .generateJwtToken(eq(testUser), eq(testCompany.getId().toString()));
        
        // And: Response should be successful
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * Test that JWT contains tenant_id when user has single membership
     */
    @Test
    void testLoginAutoSetsDefaultTenantForSingleMembership() throws Exception {
        // Given: User with no default tenant but one active membership
        testUser.setDefaultTenantId(null);
        
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication);
        
        // Create single membership
        CompanyStoreUser membership = new CompanyStoreUser(testCompany, testUser, CompanyRole.EMPLOYEE);
        membership.setIsActive(true);
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(memberships);
        
        // Mock JWT generation
        String expectedJwt = "jwt_token_with_tenant_id";
        when(jwtUtils.generateJwtToken(eq(testUser), eq(testCompany.getId().toString())))
            .thenReturn(expectedJwt);
        
        // Mock MFA check
        when(mfaService.isMfaEnabled(testUser)).thenReturn(false);
        
        // When: User logs in
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Then: User's defaultTenantId should be set to the single membership
        verify(userService, times(1)).saveUser(argThat(user -> 
            user.getDefaultTenantId() != null && 
            user.getDefaultTenantId().equals(testCompany.getId())
        ));
        
        // And: JWT should be generated with tenant_id
        verify(jwtUtils, times(1))
            .generateJwtToken(eq(testUser), eq(testCompany.getId().toString()));
        
        // And: Response should be successful
        assertEquals(200, response.getStatusCodeValue());
    }

    /**
     * Test that login with multiple memberships requires tenant selection
     */
    @Test
    void testLoginWithMultipleMembershipsRequiresSelection() throws Exception {
        // Given: User with no default tenant and multiple memberships
        testUser.setDefaultTenantId(null);
        
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication);
        
        // Create multiple memberships
        Company company2 = new Company();
        company2.setId(UUID.randomUUID());
        company2.setName("Company 2");
        
        CompanyStoreUser membership1 = new CompanyStoreUser(testCompany, testUser, CompanyRole.FOUNDER);
        membership1.setIsActive(true);
        
        CompanyStoreUser membership2 = new CompanyStoreUser(company2, testUser, CompanyRole.EMPLOYEE);
        membership2.setIsActive(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership1);
        memberships.add(membership2);
        
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(testUser))
            .thenReturn(memberships);
        
        // Mock MFA check
        when(mfaService.isMfaEnabled(testUser)).thenReturn(false);
        
        // When: User logs in
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Then: Response should be CONFLICT (409) with tenant selection required
        assertEquals(409, response.getStatusCodeValue());
        
        // And: JWT should NOT be generated
        verify(jwtUtils, never()).generateJwtToken(any(), any());
    }
}
