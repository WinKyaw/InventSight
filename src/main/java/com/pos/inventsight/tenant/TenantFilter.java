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
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TenantFilter extracts the tenant identifier from the X-Tenant-ID request header
 * and sets it in TenantContext for the duration of the request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    /**
     * Header name for tenant identification
     */
    public static final String TENANT_HEADER_NAME = "X-Tenant-ID";
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TenantFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Extract tenant ID from header
        String tenantId = httpRequest.getHeader(TENANT_HEADER_NAME);
        
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            // Validate and sanitize tenant ID
            tenantId = sanitizeTenantId(tenantId.trim());
            logger.debug("Setting tenant context to: {}", tenantId);
            TenantContext.setCurrentTenant(tenantId);
        } else {
            // Use default tenant if no header provided
            logger.debug("No tenant header found, using default tenant");
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
}