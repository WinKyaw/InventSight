package com.pos.inventsight.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RateLimitingFilter to verify auth endpoints are excluded from rate limiting
 */
class RateLimitingFilterTest {

    @Test
    void testAuthEndpointsAreSkipped() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        
        // Test /api/auth/login endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Should not rate limit - filter chain should be called
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status
        assertNotEquals(429, response.getStatus());
    }
    
    @Test
    void testAuthRegisterEndpointIsSkipped() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        
        // Test /api/auth/register endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Should not rate limit - filter chain should be called
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status
        assertNotEquals(429, response.getStatus());
    }
    
    @Test
    void testHealthEndpointIsSkipped() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        
        // Test /actuator/health endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Should not rate limit - filter chain should be called
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status
        assertNotEquals(429, response.getStatus());
    }
    
    @Test
    void testPublicEndpointIsSkipped() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        
        // Test /api/public/test endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/test");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        // Should not rate limit - filter chain should be called
        filter.doFilter(request, response, chain);
        
        // Verify no 429 status
        assertNotEquals(429, response.getStatus());
    }
}
