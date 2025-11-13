package com.pos.inventsight.config;

import com.pos.inventsight.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                System.out.println("🔍 InventSight - JWT token found, validating...");
                
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    String tenantId = jwtUtils.getTenantIdFromJwtToken(jwt);
                    
                    System.out.println("✅ InventSight - JWT validation successful");
                    System.out.println("👤 Username: " + username);
                    System.out.println("🏢 Tenant ID: " + tenantId);
                    
                    if (username == null || username.isEmpty()) {
                        System.out.println("❌ InventSight - Username is null or empty in JWT");
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    if (userDetails == null) {
                        System.out.println("❌ InventSight - User not found: " + username);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("✅ InventSight - Authentication set successfully for: " + username);
                } else {
                    System.out.println("❌ InventSight - JWT token validation failed");
                }
            } else {
                System.out.println("⚠️ InventSight - No JWT token found in Authorization header");
            }
        } catch (UsernameNotFoundException e) {
            System.out.println("❌ InventSight - User not found: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ InventSight - Authentication error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        
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