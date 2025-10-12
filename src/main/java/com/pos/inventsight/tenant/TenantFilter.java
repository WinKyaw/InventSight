package com.pos.inventsight.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.pos.inventsight.model.sql.User;

import java.io.IOException;

/**
 * TenantFilter - LEGACY filter made no-op for protected routes.
 * CompanyTenantFilter is now the authoritative source for tenant context on protected endpoints.
 * This filter only handles public endpoints where tenant context might be needed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    /**
     * Header name for tenant identification
     */
    public static final String TENANT_HEADER_NAME = "X-Tenant-ID";
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TenantFilter initialized (legacy - no-op for protected routes)");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();
        
        // Skip all protected endpoints - CompanyTenantFilter handles these
        if (isProtectedEndpoint(requestUri)) {
            logger.debug("Skipping TenantFilter for protected endpoint: {} - CompanyTenantFilter handles this", requestUri);
            chain.doFilter(request, response);
            return;
        }
        
        // Only handle public endpoints
        // First, try to get tenant ID from authenticated user
        String tenantId = getTenantIdFromAuthenticatedUser();
        
        // If no authenticated user, fall back to header-based approach
        if (tenantId == null) {
            tenantId = httpRequest.getHeader(TENANT_HEADER_NAME);
        }
        
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            // Validate and sanitize tenant ID
            tenantId = sanitizeTenantId(tenantId.trim());
            logger.debug("Setting tenant context to: {} (public endpoint)", tenantId);
            TenantContext.setCurrentTenant(tenantId);
        } else {
            // Use default tenant if no header provided and no authenticated user
            logger.debug("No tenant found from user or header, using default tenant");
            TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        }
        
        try {
            // Continue with the filter chain
            chain.doFilter(request, response);
        } finally {
            // Always clear the tenant context after request processing
            logger.debug("Clearing tenant context");
            TenantContext.clear();
        }
    }
    
    /**
     * Check if the endpoint is protected (should be handled by CompanyTenantFilter)
     * @param requestUri the request URI
     * @return true if protected endpoint, false otherwise
     */
    private boolean isProtectedEndpoint(String requestUri) {
        // All endpoints except public ones are protected
        return !isPublicEndpoint(requestUri);
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication)
     * @param requestUri the request URI
     * @return true if public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String requestUri) {
        return requestUri.startsWith("/auth/") ||
               requestUri.startsWith("/api/register") ||
               requestUri.startsWith("/api/auth/register") ||
               requestUri.startsWith("/api/auth/signup") ||
               requestUri.startsWith("/register") ||
               requestUri.startsWith("/health") ||
               requestUri.startsWith("/actuator") ||
               requestUri.startsWith("/swagger-ui") ||
               requestUri.startsWith("/v3/api-docs") ||
               requestUri.startsWith("/docs") ||
               requestUri.equals("/favicon.ico");
    }
    
    @Override
    public void destroy() {
        logger.info("TenantFilter destroyed");
    }
    
    /**
     * Sanitize tenant ID to prevent security issues
     * @param tenantId the raw tenant ID from header
     * @return sanitized tenant ID or default if invalid
     */
    private String sanitizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return TenantContext.DEFAULT_TENANT;
        }
        
        // Remove any potentially dangerous characters
        // Allow only alphanumeric, underscore, and dash
        String sanitized = tenantId.replaceAll("[^a-zA-Z0-9_-]", "");
        
        // Ensure it's not empty after sanitization
        if (sanitized.isEmpty()) {
            logger.warn("Tenant ID '{}' became empty after sanitization, using default", tenantId);
            return TenantContext.DEFAULT_TENANT;
        }
        
        // Limit length to prevent issues
        if (sanitized.length() > 63) {
            logger.warn("Tenant ID '{}' too long, truncating", sanitized);
            sanitized = sanitized.substring(0, 63);
        }
        
        // Don't allow certain reserved names
        if ("public".equalsIgnoreCase(sanitized) || 
            "information_schema".equalsIgnoreCase(sanitized) ||
            "pg_catalog".equalsIgnoreCase(sanitized)) {
            logger.debug("Using tenant ID '{}' as-is (reserved schema)", sanitized);
        }
        
        return sanitized.toLowerCase();
    }
    
    /**
     * Extract tenant ID from authenticated user if available
     * @return the user's UUID as tenant ID, or null if no authenticated user
     */
    private String getTenantIdFromAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Check if user is authenticated and not anonymous
            if (authentication != null && 
                authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof User) {
                
                User user = (User) authentication.getPrincipal();
                if (user.getUuid() != null) {
                    String userTenantId = user.getUuid().toString();
                    logger.debug("Found authenticated user with UUID: {}", userTenantId);
                    return userTenantId;
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting tenant from authenticated user: {}", e.getMessage());
        }
        
        return null;
    }
}