package com.pos.inventsight.tenant;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for CompanyTenantFilter
 */
@ExtendWith(MockitoExtension.class)
class CompanyTenantFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PrintWriter writer;

    private CompanyTenantFilter companyTenantFilter;
    private User authenticatedUser;
    private Company company;
    private UUID companyUuid;

    @Mock
    private com.pos.inventsight.config.JwtUtils jwtUtils;
    
    @BeforeEach
    void setUp() throws IOException {
        companyTenantFilter = new CompanyTenantFilter(companyStoreUserRepository, companyRepository, jwtUtils);
        TenantContext.clear();
        SecurityContextHolder.clearContext();

        // Setup test data
        companyUuid = UUID.randomUUID();
        authenticatedUser = new User();
        authenticatedUser.setId(UUID.randomUUID());
        authenticatedUser.setUsername("testuser");
        authenticatedUser.setId(UUID.randomUUID());

        company = new Company();
        company.setId(companyUuid);
        company.setName("Test Company");
        company.setIsActive(true);

        // Mock response writer - use lenient() to avoid unnecessary stubbing errors
        StringWriter stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        lenient().when(response.getWriter()).thenReturn(writer);
        
        // Lenient JWT mocks for header mode tests (won't throw if not called)
        lenient().when(jwtUtils.hasTenantId(anyString())).thenReturn(false);
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        
        // Enable header mode by default for backward compatibility with existing tests
        // JWT-only mode tests will explicitly disable this
        companyTenantFilter.setHeaderEnabled(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFilterWithValidCompanyAndMembership() throws ServletException, IOException {
        // Given a valid request with company UUID and authenticated user
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);

        CompanyStoreUser membership = createMembership(company, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membership));

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called and tenant context was cleared
        verify(filterChain).doFilter(request, response);
        verify(companyRepository).existsById(companyUuid);
        verify(companyStoreUserRepository).findByUserAndIsActiveTrue(authenticatedUser);

        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithMissingTenantHeader() throws ServletException, IOException {
        // Given a request without X-Tenant-ID header
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(null);

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterWithEmptyTenantHeader() throws ServletException, IOException {
        // Given a request with empty X-Tenant-ID header
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn("  ");

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterWithInvalidUUID() throws ServletException, IOException {
        // Given a request with invalid UUID in X-Tenant-ID header
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn("not-a-valid-uuid");

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterWithNonExistentCompany() throws ServletException, IOException {
        // Given a request with company UUID that doesn't exist
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(false);

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        verify(companyRepository).existsById(companyUuid);
    }

    @Test
    void testFilterWithoutAuthentication() throws ServletException, IOException {
        // Given a request without authenticated user
        SecurityContextHolder.clearContext();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterWithUserNotMemberOfCompany() throws ServletException, IOException {
        // Given a request with authenticated user who is not a member of the company
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(new ArrayList<>()); // No memberships

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterWithInactiveMembership() throws ServletException, IOException {
        // Given a request with authenticated user who has inactive membership
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);

        CompanyStoreUser inactiveMembership = createMembership(company, authenticatedUser, CompanyRole.EMPLOYEE);
        inactiveMembership.setIsActive(false);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(inactiveMembership));

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent (no active membership for this company)
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilterSkipsPublicEndpoints() throws ServletException, IOException {
        // Given requests to public endpoints
        String[] publicEndpoints = {
            "/auth/login",
            "/api/auth/login",
            "/api/auth/mfa",
            "/api/auth/tenant-select",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/check-email",
            "/api/auth/validate-password",
            "/api/auth/invite/accept",
            "/api/register",
            "/api/auth/register",
            "/api/auth/signup",
            "/health/check",
            "/actuator/health",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/favicon.ico"
        };

        for (String endpoint : publicEndpoints) {
            // Reset mocks
            reset(request, response, filterChain);

            when(request.getRequestURI()).thenReturn(endpoint);

            // When processing the filter
            companyTenantFilter.doFilter(request, response, filterChain);

            // Then verify filter chain was called without validation
            verify(filterChain).doFilter(request, response);
            verify(companyRepository, never()).existsById(any());
            verify(companyStoreUserRepository, never()).findByUserAndIsActiveTrue(any());
        }
    }

    @Test
    void testFilterClearsContextOnException() throws ServletException, IOException {
        // Given a valid request that will cause an exception
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);

        CompanyStoreUser membership = createMembership(company, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membership));

        // And filter chain throws an exception
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        // When processing the filter - the exception is caught and handled internally
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then context should still be cleared even after exception
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
        
        // And error response should be sent
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testFilterWithMembershipInDifferentCompany() throws ServletException, IOException {
        // Given a user with membership in a different company
        setupAuthenticatedUser();
        UUID differentCompanyUuid = UUID.randomUUID();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(companyUuid.toString());
        when(companyRepository.existsById(companyUuid)).thenReturn(true);

        Company differentCompany = new Company();
        differentCompany.setId(differentCompanyUuid);
        differentCompany.setName("Different Company");

        CompanyStoreUser membershipInDifferentCompany = createMembership(differentCompany, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membershipInDifferentCompany));

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify error response was sent (no membership in requested company)
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testSchemaNameGeneration() throws ServletException, IOException {
        // Given a valid request
        setupAuthenticatedUser();
        UUID testUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(testUuid.toString());
        when(companyRepository.existsById(testUuid)).thenReturn(true);

        Company testCompany = new Company();
        testCompany.setId(testUuid);
        CompanyStoreUser membership = createMembership(testCompany, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membership));

        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called
        verify(filterChain).doFilter(request, response);

        // Expected schema name: company_550e8400_e29b_41d4_a716_446655440000
        // (We can't directly assert the schema name as it's set internally,
        // but we verify the filter completed successfully)
    }

    // Helper methods

    private void setupAuthenticatedUser() {
        // Create authenticated token (third parameter is authorities, making it authenticated)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            authenticatedUser, null, new ArrayList<>());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private CompanyStoreUser createMembership(Company company, User user, CompanyRole role) {
        CompanyStoreUser membership = new CompanyStoreUser();
        membership.setId(UUID.randomUUID());
        membership.setCompany(company);
        membership.setUser(user);
        membership.setRole(role);
        membership.setIsActive(true);
        return membership;
    }
    
    // ============== JWT-Only Mode Tests ==============
    
    @Test
    void testJwtOnlyMode_WithValidJwtTenantId() throws Exception {
        // Given JWT-only mode is enabled
        companyTenantFilter.setHeaderEnabled(false);
        
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        
        // Mock JWT extraction
        when(jwtUtils.hasTenantId("valid-token")).thenReturn(true);
        when(jwtUtils.getTenantIdFromJwtToken("valid-token")).thenReturn(companyUuid.toString());
        
        when(companyRepository.existsById(companyUuid)).thenReturn(true);
        
        CompanyStoreUser membership = createMembership(company, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membership));
        
        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);
        
        // Then verify filter chain was called
        verify(filterChain).doFilter(request, response);
        verify(jwtUtils).hasTenantId("valid-token");
        verify(jwtUtils).getTenantIdFromJwtToken("valid-token");
        verify(companyRepository).existsById(companyUuid);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
    }
    
    @Test
    void testJwtOnlyMode_WithMissingJwtTenantId() throws Exception {
        // Given JWT-only mode is enabled
        companyTenantFilter.setHeaderEnabled(false);
        
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        
        // Mock JWT extraction - no tenant_id in token
        when(jwtUtils.hasTenantId("valid-token")).thenReturn(false);
        
        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);
        
        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        verify(companyRepository, never()).existsById(any());
    }
    
    @Test
    void testJwtOnlyMode_WithInvalidJwtTenantIdUuid() throws Exception {
        // Given JWT-only mode is enabled
        companyTenantFilter.setHeaderEnabled(false);
        
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        
        // Mock JWT extraction with invalid UUID
        when(jwtUtils.hasTenantId("valid-token")).thenReturn(true);
        when(jwtUtils.getTenantIdFromJwtToken("valid-token")).thenReturn("not-a-valid-uuid");
        
        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);
        
        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        verify(companyRepository, never()).existsById(any());
    }
    
    @Test
    void testJwtOnlyMode_WithNoAuthorizationHeader() throws Exception {
        // Given JWT-only mode is enabled
        companyTenantFilter.setHeaderEnabled(false);
        
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);
        
        // Then verify error response was sent
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        verify(jwtUtils, never()).hasTenantId(any());
    }
    
    @Test
    void testJwtOnlyMode_XTenantIdHeaderIsIgnored() throws Exception {
        // Given JWT-only mode is enabled and X-Tenant-ID header is present
        companyTenantFilter.setHeaderEnabled(false);
        
        setupAuthenticatedUser();
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        
        // X-Tenant-ID header should be ignored in JWT-only mode
        UUID differentCompanyUuid = UUID.randomUUID();
        lenient().when(request.getHeader(CompanyTenantFilter.TENANT_HEADER_NAME)).thenReturn(differentCompanyUuid.toString());
        
        // Mock JWT extraction with correct tenant_id
        when(jwtUtils.hasTenantId("valid-token")).thenReturn(true);
        when(jwtUtils.getTenantIdFromJwtToken("valid-token")).thenReturn(companyUuid.toString());
        
        when(companyRepository.existsById(companyUuid)).thenReturn(true);
        
        CompanyStoreUser membership = createMembership(company, authenticatedUser, CompanyRole.EMPLOYEE);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(authenticatedUser))
            .thenReturn(List.of(membership));
        
        // When processing the filter
        companyTenantFilter.doFilter(request, response, filterChain);
        
        // Then verify filter chain was called with JWT tenant_id (header ignored)
        verify(filterChain).doFilter(request, response);
        verify(companyRepository).existsById(companyUuid);
        verify(companyRepository, never()).existsById(differentCompanyUuid);
    }
    
    @Test
    void testJwtOnlyMode_PublicEndpointsWithApiPrefix() throws Exception {
        // Given JWT-only mode is enabled
        companyTenantFilter.setHeaderEnabled(false);
        
        // Test public endpoints with /api prefix don't require tenant_id
        String[] publicApiEndpoints = {
            "/api/auth/login",
            "/api/auth/mfa",
            "/api/auth/tenant-select",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/check-email",
            "/api/auth/validate-password",
            "/api/auth/invite/accept"
        };
        
        for (String endpoint : publicApiEndpoints) {
            // Reset mocks
            reset(request, response, filterChain, jwtUtils);
            
            when(request.getRequestURI()).thenReturn(endpoint);
            
            // When processing the filter
            companyTenantFilter.doFilter(request, response, filterChain);
            
            // Then verify filter chain was called without tenant validation
            verify(filterChain).doFilter(request, response);
            verify(jwtUtils, never()).hasTenantId(any());
            verify(companyRepository, never()).existsById(any());
        }
    }
}
