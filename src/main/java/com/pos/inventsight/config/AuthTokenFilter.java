package com.pos.inventsight.config;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyStoreUserRole;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRoleRepository;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.util.RoleUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userService;

    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Autowired
    private CompanyStoreUserRoleRepository companyStoreUserRoleRepository;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        // Log every invocation
        logger.debug("🔍 shouldNotFilter() called for: {} {}", method, requestUri);
        
        // Skip filter for truly public endpoints only
        boolean shouldSkip = isPublicEndpoint(requestUri);
        
        if (shouldSkip) {
            logger.debug("⏭️  WILL SKIP request (public endpoint): {} {}", method, requestUri);
        } else {
            logger.debug("⚡ Processing request: {} {}", method, requestUri);
        }
        
        return shouldSkip;
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication)
     * This should match the permitAll() rules in SecurityConfig
     * @param requestUri the request URI
     * @return true if public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String requestUri) {
        // Handle null requestUri gracefully
        if (requestUri == null) {
            return false;
        }
        
        // Authentication endpoints - public
        if (requestUri.startsWith("/auth/") || 
            requestUri.startsWith("/api/auth/") ||
            requestUri.startsWith("/register")) {
            return true;
        }
        
        // OAuth2 and login endpoints - public
        if (requestUri.startsWith("/oauth2/") || 
            requestUri.startsWith("/login/")) {
            return true;
        }
        
        // Health, monitoring, and documentation endpoints - public
        if (requestUri.startsWith("/health") ||
            requestUri.startsWith("/actuator") ||
            requestUri.startsWith("/swagger-ui") ||
            requestUri.startsWith("/v3/api-docs") ||
            requestUri.startsWith("/docs") ||
            requestUri.startsWith("/dashboard/live-data") ||
            requestUri.equals("/favicon.ico")) {
            return true;
        }
        
        // All other endpoints require authentication (including business APIs)
        // Such as: /products, /stores, /sales, /inventory, etc.
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("=== AuthTokenFilter START === Request: {} {}", method, requestUri);
        logger.debug("Authorization header present: {}", request.getHeader("Authorization") != null);
        
        try {
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                logger.debug("JWT token extracted from Authorization header (length: {})", jwt.length());
                logger.debug("🔐 Validating JWT token...");
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    String tenantId = jwtUtils.getTenantIdFromJwtToken(jwt);
                    
                    logger.debug("✅ JWT validation successful for user: {}", username);
                    logger.debug("Tenant ID from JWT: {}", tenantId);
                    
                    if (username == null || username.isEmpty()) {
                        logger.warn("❌ Username is null or empty in JWT token");
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    logger.debug("Loading user details for username: {}", username);
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    if (userDetails == null) {
                        logger.warn("❌ User not found in database: {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    logger.debug("User details loaded successfully. User type: {}", userDetails.getClass().getSimpleName());
                    logger.debug("User authorities: {}", userDetails.getAuthorities());

                    User user = (User) userDetails;
                    Collection<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());

                    // Upgrade authority from active company role when the global role is not already GM+
                    if (!RoleUtils.isGMPlusRole(user.getRole())) {
                        List<Company> companies = companyStoreUserRepository.findCompaniesByUser(user);
                        if (!companies.isEmpty()) {
                            // Use the first active company as the primary context.
                            // In multi-company scenarios the full resolution strategy is handled
                            // by downstream services; this filter only needs to determine
                            // whether the user has at least GM+ access in any assigned company.
                            Company primaryCompany = companies.get(0);
                            List<CompanyStoreUserRole> roles = companyStoreUserRoleRepository
                                .findByUserAndCompanyAndIsActiveTrue(user, primaryCompany);
                            List<CompanyStoreUserRole> validRoles = roles.stream()
                                .filter(r -> !RoleUtils.isExpired(r))
                                .collect(Collectors.toList());
                            if (!validRoles.isEmpty()) {
                                CompanyStoreUserRole highestRole = RoleUtils.getHighestPriorityRole(validRoles);
                                String mappedRole = RoleUtils.mapCompanyRoleToUserRole(highestRole.getRole());
                                logger.debug("Upgrading authority for user {} from {} to {} (via company role {})",
                                    username, user.getRole(), mappedRole, highestRole.getRole());
                                // Replace the single global-role authority with the mapped company role.
                                // User.getAuthorities() always returns exactly one SimpleGrantedAuthority,
                                // so no non-role permissions are discarded.
                                authorities = new ArrayList<>();
                                authorities.add(new SimpleGrantedAuthority(mappedRole));
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    logger.debug("🔑 Setting authentication in SecurityContext");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Enhanced verification logging
                    Authentication verifyAuth = SecurityContextHolder.getContext().getAuthentication();
                    logger.debug("✅ Authentication SET and VERIFIED in SecurityContext: {}", verifyAuth != null);
                    if (verifyAuth != null) {
                        logger.debug("  - Principal class: {}", verifyAuth.getPrincipal().getClass().getName());
                        logger.debug("  - Is authenticated: {}", verifyAuth.isAuthenticated());
                        logger.debug("  - Authorities: {}", verifyAuth.getAuthorities());
                        logger.debug("  - SecurityContext hashCode: {}", System.identityHashCode(SecurityContextHolder.getContext()));
                    }
                    logger.debug("✅ Authentication successful for user: {} on {} {}", username, method, requestUri);
                } else {
                    logger.warn("⚠️ Invalid JWT token for request: {} {}", method, requestUri);
                }
            } else {
                logger.debug("No JWT token found in Authorization header for: {} {}", method, requestUri);
            }
        } catch (UsernameNotFoundException e) {
            logger.error("❌ User not found during authentication: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Authentication error: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }
        
        logger.debug("=== AuthTokenFilter END === Proceeding to next filter");
        filterChain.doFilter(request, response);
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        
        return null;
    }
}