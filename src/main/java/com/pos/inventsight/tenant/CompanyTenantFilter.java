package com.pos.inventsight.tenant;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * CompanyTenantFilter validates X-Tenant-ID header containing company UUID
 * and ensures the authenticated user has membership in that company.
 * Sets PostgreSQL search_path to company schema (company_<uuid>) with no public fallback.
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
    
    public CompanyTenantFilter(CompanyStoreUserRepository companyStoreUserRepository,
                               CompanyRepository companyRepository) {
        this.companyStoreUserRepository = companyStoreUserRepository;
        this.companyRepository = companyRepository;
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
            // Extract X-Tenant-ID header (expecting company UUID)
            String tenantHeader = httpRequest.getHeader(TENANT_HEADER_NAME);
            
            if (tenantHeader == null || tenantHeader.trim().isEmpty()) {
                logger.warn("Missing X-Tenant-ID header for request: {}", requestUri);
                sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                         "X-Tenant-ID header is required and must contain a valid company UUID");
                return;
            }
            
            // Parse and validate company UUID
            UUID companyUuid;
            try {
                companyUuid = UUID.fromString(tenantHeader.trim());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid company UUID in X-Tenant-ID header: {}", tenantHeader);
                sendError(httpResponse, HttpServletResponse.SC_BAD_REQUEST, 
                         "X-Tenant-ID must be a valid UUID");
                return;
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
}
