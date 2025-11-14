package com.pos.inventsight.config;

import com.pos.inventsight.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userService;
    
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
                logger.debug("Validating JWT token...");
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    String tenantId = jwtUtils.getTenantIdFromJwtToken(jwt);
                    
                    logger.debug("JWT validation successful");
                    logger.debug("Username from JWT: {}", username);
                    logger.debug("Tenant ID from JWT: {}", tenantId);
                    
                    if (username == null || username.isEmpty()) {
                        logger.warn("Username is null or empty in JWT token");
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    logger.debug("Loading user details for username: {}", username);
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    if (userDetails == null) {
                        logger.warn("User not found in database: {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    logger.debug("User details loaded successfully. User type: {}", userDetails.getClass().getSimpleName());
                    logger.debug("User authorities: {}", userDetails.getAuthorities());
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    logger.debug("Setting authentication in SecurityContext");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Verify authentication was set
                    boolean isSet = SecurityContextHolder.getContext().getAuthentication() != null;
                    logger.debug("Authentication set in SecurityContext: {}", isSet);
                    logger.debug("Principal type: {}", 
                        SecurityContextHolder.getContext().getAuthentication() != null ? 
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal().getClass().getSimpleName() : "null");
                    logger.info("Authentication successful for user: {} on {} {}", username, method, requestUri);
                } else {
                    logger.warn("JWT token validation failed for request: {} {}", method, requestUri);
                }
            } else {
                logger.debug("No JWT token found in Authorization header for: {} {}", method, requestUri);
            }
        } catch (UsernameNotFoundException e) {
            logger.error("User not found during authentication: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Authentication error: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
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