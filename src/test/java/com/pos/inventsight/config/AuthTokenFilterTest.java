package com.pos.inventsight.config;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRoleRepository;
import com.pos.inventsight.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthTokenFilter to verify shouldNotFilter logic
 */
@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Mock
    private CompanyStoreUserRoleRepository companyStoreUserRoleRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_publicAuthEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/auth/login");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /auth/login endpoint");
    }

    @Test
    void shouldNotFilter_publicApiAuthEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /api/auth/register endpoint");
    }

    @Test
    void shouldNotFilter_healthEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/health/check");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /health endpoint");
    }

    @Test
    void shouldNotFilter_swaggerEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /swagger-ui endpoint");
    }

    @Test
    void shouldNotFilter_productsEndpoint_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/products");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip /products endpoint");
    }

    @Test
    void shouldNotFilter_storesEndpoint_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/stores");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip /stores endpoint");
    }

    @Test
    void shouldNotFilter_salesEndpoint_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/sales");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip /sales endpoint");
    }

    @Test
    void shouldNotFilter_inventoryEndpoint_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/inventory");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip /inventory endpoint");
    }

    @Test
    void shouldNotFilter_apiProductsEndpoint_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/products");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip /api/products endpoint (context-path is handled by Spring)");
    }

    @Test
    void shouldNotFilter_nullRequestUri_returnsFalse() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn(null);

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertFalse(result, "AuthTokenFilter should NOT skip when requestUri is null");
    }

    @Test
    void shouldNotFilter_oauth2Endpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/oauth2/callback");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /oauth2 endpoint");
    }

    @Test
    void shouldNotFilter_loginEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/login/oauth2");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /login endpoint");
    }

    @Test
    void shouldNotFilter_actuatorEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /actuator endpoint");
    }

    @Test
    void shouldNotFilter_faviconEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /favicon.ico endpoint");
    }

    @Test
    void shouldNotFilter_dashboardLiveDataEndpoint_returnsTrue() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/dashboard/live-data");

        // When
        boolean result = ReflectionTestUtils.invokeMethod(authTokenFilter, "shouldNotFilter", request);

        // Then
        assertTrue(result, "AuthTokenFilter should skip /dashboard/live-data endpoint");
    }

    // ── Authority upgrade tests ──────────────────────────────────────────────

    @Test
    void doFilterInternal_employeeWithGeneralManagerCompanyRole_setsManagerAuthority()
            throws ServletException, IOException {
        // Given - a user with global EMPLOYEE role
        User employee = new User("employee", "employee@example.com", "pw", "Jane", "Doe");
        employee.setRole(UserRole.EMPLOYEE);

        String jwt = "test.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(request.getRequestURI()).thenReturn("/api/dashboard/summary");
        when(request.getMethod()).thenReturn("GET");
        when(jwtUtils.validateJwtToken(jwt)).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken(jwt)).thenReturn("employee");
        when(jwtUtils.getTenantIdFromJwtToken(jwt)).thenReturn("tenant-1");
        when(userService.loadUserByUsername("employee")).thenReturn(employee);

        Company company = new Company();
        when(companyStoreUserRepository.findCompaniesByUser(employee)).thenReturn(List.of(company));

        CompanyStoreUserRole gmRole = new CompanyStoreUserRole();
        gmRole.setRole(CompanyRole.GENERAL_MANAGER);
        gmRole.setIsActive(true);
        when(companyStoreUserRoleRepository.findByUserAndCompanyAndIsActiveTrue(employee, company))
            .thenReturn(List.of(gmRole));

        // When
        ReflectionTestUtils.invokeMethod(authTokenFilter, "doFilterInternal", request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext");
        assertTrue(
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("MANAGER")),
            "Employee with GENERAL_MANAGER company role should have MANAGER authority");
    }

    @Test
    void doFilterInternal_employeeWithNoCompanyRole_keepsEmployeeAuthority()
            throws ServletException, IOException {
        // Given - a user with global EMPLOYEE role and no active company role
        User employee = new User("employee2", "employee2@example.com", "pw", "John", "Smith");
        employee.setRole(UserRole.EMPLOYEE);

        String jwt = "test.jwt.token2";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(request.getRequestURI()).thenReturn("/api/dashboard/summary");
        when(request.getMethod()).thenReturn("GET");
        when(jwtUtils.validateJwtToken(jwt)).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken(jwt)).thenReturn("employee2");
        when(jwtUtils.getTenantIdFromJwtToken(jwt)).thenReturn("tenant-1");
        when(userService.loadUserByUsername("employee2")).thenReturn(employee);

        when(companyStoreUserRepository.findCompaniesByUser(employee)).thenReturn(List.of());

        // When
        ReflectionTestUtils.invokeMethod(authTokenFilter, "doFilterInternal", request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext");
        assertTrue(
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("EMPLOYEE")),
            "Employee with no company role should keep EMPLOYEE authority");
        assertFalse(
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("MANAGER")),
            "Employee with no GM+ company role must not be upgraded to MANAGER");
    }

    @Test
    void doFilterInternal_managerGlobalRole_notDowngradedByCompanyRole()
            throws ServletException, IOException {
        // Given - a user already with global MANAGER role (GM+); company role lookup should be skipped
        User manager = new User("manager", "manager@example.com", "pw", "Bob", "Lee");
        manager.setRole(UserRole.MANAGER);

        String jwt = "test.jwt.token3";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(request.getRequestURI()).thenReturn("/api/dashboard/summary");
        when(request.getMethod()).thenReturn("GET");
        when(jwtUtils.validateJwtToken(jwt)).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken(jwt)).thenReturn("manager");
        when(jwtUtils.getTenantIdFromJwtToken(jwt)).thenReturn("tenant-1");
        when(userService.loadUserByUsername("manager")).thenReturn(manager);

        // When
        ReflectionTestUtils.invokeMethod(authTokenFilter, "doFilterInternal", request, response, filterChain);

        // Then - company role repos must NOT be consulted for GM+ global roles
        verify(companyStoreUserRepository, never()).findCompaniesByUser(any());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext");
        assertTrue(
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("MANAGER")),
            "User with global MANAGER role should keep MANAGER authority");
    }
}
