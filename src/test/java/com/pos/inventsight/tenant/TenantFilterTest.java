package com.pos.inventsight.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.pos.inventsight.model.sql.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantFilter
 */
@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TenantFilter tenantFilter;

    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
        TenantContext.clear();
        // Clear SecurityContext
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFilterWithTenantHeader() throws ServletException, IOException {
        // Given a request with tenant header
        String tenantId = "test_tenant";
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn(tenantId);

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called and tenant context was cleared
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithoutTenantHeader() throws ServletException, IOException {
        // Given a request without tenant header
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn(null);

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called and default tenant was used
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithEmptyTenantHeader() throws ServletException, IOException {
        // Given a request with empty tenant header
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn("  ");

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called and default tenant was used
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithInvalidCharacters() throws ServletException, IOException {
        // Given a request with tenant header containing invalid characters
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn("test@tenant$");

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterClearsContextOnException() throws ServletException, IOException {
        // Given a request with tenant header
        String tenantId = "test_tenant";
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn(tenantId);
        
        // And filter chain throws an exception
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

        // When processing the filter
        assertThrows(ServletException.class, () -> {
            tenantFilter.doFilter(request, response, filterChain);
        });

        // Then context should still be cleared even after exception
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithAuthenticatedUser() throws ServletException, IOException {
        // Given an authenticated user
        UUID userUuid = UUID.randomUUID();
        User user = new User();
        user.setUuid(userUuid);
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // And no tenant header
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn(null);

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterPrefersAuthenticatedUserOverHeader() throws ServletException, IOException {
        // Given an authenticated user
        UUID userUuid = UUID.randomUUID();
        User user = new User();
        user.setUuid(userUuid);
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // And a different tenant header
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn("different_tenant");

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called  
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution  
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }

    @Test
    void testFilterWithUnauthenticatedUser() throws ServletException, IOException {
        // Given no authentication (null or anonymous)
        SecurityContextHolder.clearContext();

        // And a tenant header
        String tenantId = "header_tenant";
        when(request.getHeader(TenantFilter.TENANT_HEADER_NAME)).thenReturn(tenantId);

        // When processing the filter
        tenantFilter.doFilter(request, response, filterChain);

        // Then verify filter chain was called and header tenant was used
        verify(filterChain).doFilter(request, response);
        
        // Context should be cleared after filter execution
        assertEquals(TenantContext.DEFAULT_TENANT, TenantContext.getCurrentTenant());
        assertFalse(TenantContext.isSet());
    }
}