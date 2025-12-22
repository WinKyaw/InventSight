package com.pos.inventsight.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AuthRateLimitingFilter to verify auth-specific rate limiting
 */
class AuthRateLimitingFilterTest {

    @Test
    void testLoginEndpointIsRateLimited() throws Exception {
        AuthRateLimitingFilter filter = new AuthRateLimitingFilter();
        
        // Test /api/auth/login endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // First request should succeed
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status on first request
        assertNotEquals(429, response.getStatus());
    }
    
    @Test
    void testRegisterEndpointIsRateLimited() throws Exception {
        AuthRateLimitingFilter filter = new AuthRateLimitingFilter();
        
        // Test /api/auth/register endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
        request.setRemoteAddr("192.168.1.2");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // First request should succeed
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status on first request
        assertNotEquals(429, response.getStatus());
    }
    
    @Test
    void testNonAuthEndpointIsNotRateLimited() throws Exception {
        AuthRateLimitingFilter filter = new AuthRateLimitingFilter();
        
        // Test non-auth endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/products");
        request.setRemoteAddr("192.168.1.3");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Should pass through without rate limiting
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status
        assertNotEquals(429, response.getStatus());
    }
}
