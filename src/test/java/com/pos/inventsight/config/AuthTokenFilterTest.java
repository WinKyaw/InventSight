package com.pos.inventsight.config;

import com.pos.inventsight.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    void setUp() {
        // No additional setup needed for these tests
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
}
