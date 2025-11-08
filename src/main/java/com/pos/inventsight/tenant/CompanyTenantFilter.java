package com.pos.inventsight.tenant;

import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * CompanyTenantFilter validates company tenant identification from JWT token or header
 * and ensures the authenticated user has membership in that company.
 * Sets PostgreSQL search_path to company schema (company_<uuid>) with no public fallback.
 * 
 * Supports two modes:
 * - JWT-only mode (default, inventsight.tenancy.header.enabled=false): 
 *   Requires tenant_id claim in JWT token. X-Tenant-ID header is ignored.
 * - Header mode (inventsight.tenancy.header.enabled=true): 
 *   Accepts X-Tenant-ID header with optional validation against JWT tenant_id claim.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CompanyTenantFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyTenantFilter.class);
    
    /**
     * Header name for company tenant identification
     */
    public static final String TENANT_HEADER_NAME = "X-Tenant-ID";
    
    private final CompanyStoreUserRepository companyStoreUserRepository;
    private final CompanyRepository companyRepository;
    private final JwtUtils jwtUtils;
    
    @Value("${inventsight.tenancy.header.enabled:false}")
    private boolean headerEnabled;
    
    @Value("${inventsight.tenancy.header.validate-against-jwt:true}")
    private boolean validateHeaderAgainstJwt;
    
    public CompanyTenantFilter(CompanyStoreUserRepository companyStoreUserRepository,
                               CompanyRepository companyRepository,
                               JwtUtils jwtUtils) {
        this.companyStoreUserRepository = companyStoreUserRepository;
        this.companyRepository = companyRepository;
        this.jwtUtils = jwtUtils;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CompanyTenantFilter initialized - schema-per-company multi-tenancy enabled");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        
        // Skip filter for public endpoints
        if (isPublicEndpoint(requestUri)) {
            logger.debug("Skipping CompanyTenantFilter for public endpoint: {}", requestUri);
            chain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract tenant_id from JWT claim if present
            String jwtTenantId = null;
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (jwtUtils.hasTenantId(token)) {
                        jwtTenantId = jwtUtils.getTenantIdFromJwtToken(token);
                        logger.debug("Extracted tenant_id from JWT: {}", jwtTenantId);
                    }
                } catch (Exception e) {
                    logger.debug("Could not extract tenant_id from JWT: {}", e.getMessage());
                }
            }
            
            // Determine the authoritative tenant ID
            UUID companyUuid;
            
            if (!headerEnabled) {
                // JWT-only mode: require tenant_id from JWT claim
                if (jwtTenantId == null || jwtTenantId.isEmpty()) {
                    logger.warn("JWT-only mode: tenant_id claim is required but not found in JWT for request: {}", requestUri);
                    sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                             "tenant_id claim is required in JWT token");
                    return;
                }
                
                try {
                    companyUuid = UUID.fromString(jwtTenantId);
                    logger.debug("JWT-only mode: Using tenant_id from JWT: {}", companyUuid);
                } catch (IllegalArgumentException e) {
                    logger.warn("JWT-only mode: Invalid tenant_id UUID in JWT claim: {}", jwtTenantId);
                    sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                             "Invalid tenant_id UUID in JWT token");
                    return;
                }
            } else {
                // Header mode: allow X-Tenant-ID header with optional JWT validation
                String tenantHeader = httpRequest.getHeader(TENANT_HEADER_NAME);
                
                // If JWT has tenant_id, use it as source of truth
                if (jwtTenantId != null && !jwtTenantId.isEmpty()) {
                    try {
                        companyUuid = UUID.fromString(jwtTenantId);
                        logger.debug("Using tenant_id from JWT: {}", companyUuid);
                        
                        // If validateHeaderAgainstJwt is enabled and X-Tenant-ID is present, verify they match
                        if (validateHeaderAgainstJwt && tenantHeader != null && !tenantHeader.trim().isEmpty()) {
                            if (!tenantHeader.trim().equals(jwtTenantId)) {
                                logger.warn("X-Tenant-ID header {} does not match JWT tenant_id claim {}", 
                                           tenantHeader, jwtTenantId);
                                sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                                         "X-Tenant-ID header does not match authenticated tenant");
                                return;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid tenant_id UUID in JWT claim: {}", jwtTenantId);
                        sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                                 "Invalid tenant_id in JWT token");
                        return;
                    }
                } else {
                    // Fall back to X-Tenant-ID header if no JWT tenant_id
                    if (tenantHeader == null || tenantHeader.trim().isEmpty()) {
                        logger.warn("Missing X-Tenant-ID header and no tenant_id in JWT for request: {}", requestUri);
                        sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                                 "X-Tenant-ID header is required or tenant_id must be present in JWT");
                        return;
                    }
                    
                    // Parse and validate company UUID from header
                    try {
                        companyUuid = UUID.fromString(tenantHeader.trim());
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid company UUID in X-Tenant-ID header: {}", tenantHeader);
                        sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                                 "X-Tenant-ID must be a valid UUID");
                        return;
                    }
                }
            }
            
            // Verify company exists and is active
            if (!companyRepository.existsById(companyUuid)) {
                logger.warn("Company not found for UUID: {}", companyUuid);
                sendError(httpResponse, HttpServletResponse.SC_NOT_FOUND, 
                         "Company not found");
                return;
            }
            
            // Get authenticated user
            User authenticatedUser = getAuthenticatedUser();
            if (authenticatedUser == null) {
                logger.warn("No authenticated user found for company tenant request");
                sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, 
                         "Authentication required");
                return;
            }
            
            // Verify user membership in company
            List<CompanyStoreUser> memberships = companyStoreUserRepository
                .findByUserAndIsActiveTrue(authenticatedUser);
            
            boolean hasMembership = memberships.stream()
                .anyMatch(m -> m.getCompany().getId().equals(companyUuid) && m.getIsActive());
            
            if (!hasMembership) {
                logger.warn("User {} does not have membership in company {}", 
                           authenticatedUser.getUsername(), companyUuid);
                sendError(httpResponse, HttpServletResponse.SC_FORBIDDEN, 
                         "Access denied: user is not a member of the specified company");
                return;
            }
            
            // Set tenant context to company schema (company_<uuid>)
            String companySchema = buildCompanySchemaName(companyUuid);
            logger.debug("Setting company tenant context to schema: {}", companySchema);
            TenantContext.setCurrentTenant(companySchema);
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in CompanyTenantFilter: {}", e.getMessage(), e);
            sendError(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                     "Internal server error processing tenant context");
        } finally {
            // Always clear the tenant context after request processing
            logger.debug("Clearing company tenant context");
            TenantContext.clear();
        }
    }
    
    @Override
    public void destroy() {
        logger.info("CompanyTenantFilter destroyed");
    }
    
    /**
     * Build schema name for company: company_<uuid>
     * Converts UUID dashes to underscores for valid PostgreSQL schema name
     * @param companyUuid the company UUID
     * @return schema name in format: company_<uuid_with_underscores>
     */
    private String buildCompanySchemaName(UUID companyUuid) {
        // Convert UUID to string and replace dashes with underscores
        String uuidStr = companyUuid.toString().replace("-", "_");
        return "company_" + uuidStr;
    }
    
    /**
     * Check if the endpoint is public (doesn't require tenant context)
     * Note: Local login endpoints (/auth/login, /auth/register) are public when enabled,
     * but they're gated at SecurityConfig level by @ConditionalOnProperty on controllers.
     * @param requestUri the request URI
     * @return true if public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String requestUri) {
        // OAuth2 endpoints - always public
        if (requestUri.startsWith("/oauth2/") || requestUri.startsWith("/login/")) {
            return true;
        }
        
        // Local authentication endpoints - public (controller availability gated by @ConditionalOnProperty)
        if (requestUri.startsWith("/auth/login") ||
            requestUri.startsWith("/auth/register") ||
            requestUri.startsWith("/auth/signup") ||
            requestUri.startsWith("/auth/invite/accept") ||
            requestUri.startsWith("/auth/check-email") ||
            requestUri.startsWith("/auth/verify-email") ||
            requestUri.startsWith("/auth/resend-verification") ||
            requestUri.startsWith("/auth/validate-password") ||
            requestUri.startsWith("/auth/mfa")) {  // MFA endpoints don't require tenant context
            return true;
        }
        
        // Registration endpoints - public (controller availability gated by @ConditionalOnProperty)
        if (requestUri.startsWith("/api/register") ||
            requestUri.startsWith("/api/auth/register") ||
            requestUri.startsWith("/api/auth/signup") ||
            requestUri.startsWith("/register")) {
            return true;
        }
        
        // Health, monitoring, and documentation endpoints
        if (requestUri.startsWith("/health") ||
            requestUri.startsWith("/actuator") ||
            requestUri.startsWith("/swagger-ui") ||
            requestUri.startsWith("/v3/api-docs") ||
            requestUri.startsWith("/docs") ||
            requestUri.startsWith("/dashboard/live-data") ||
            requestUri.equals("/favicon.ico")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract authenticated user from security context
     * @return the authenticated user, or null if not authenticated
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && 
                authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            logger.debug("Error extracting authenticated user: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Send error response
     * @param response the HTTP response
     * @param status the HTTP status code
     * @param message the error message
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }
    
    /**
     * Package-private setter for testing purposes
     * @param enabled whether header mode is enabled
     */
    void setHeaderEnabled(boolean enabled) {
        this.headerEnabled = enabled;
    }
}
