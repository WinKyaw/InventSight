package com.pos.inventsight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC Configuration for InventSight REST API
 * Configures Spring MVC without enabling default servlet handling
 * 
 * Key features:
 * 1. Path matching configuration - ensures controllers are matched first
 * 2. Static resource handlers - ONLY specific paths to prevent API conflicts
 * 3. CORS configuration - global CORS support for API endpoints
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * Configure path matching to ensure controllers are matched correctly
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Disable trailing slash matching for stricter path matching
        configurer.setUseTrailingSlashMatch(false);
        
        // Disable suffix pattern matching (e.g., /users.json)
        configurer.setUseSuffixPatternMatch(false);
        
        System.out.println("🔧 WebMvcConfig: Path matching configured (trailing slash=false, suffix pattern=false)");
    }
    
    /**
     * Configure static resource handlers with LOWEST priority
     * Controllers should be checked BEFORE static resources
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources ONLY from /static/** path
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/");
        
        // Handle favicon specifically
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/");
        
        // ✅ FIX: Set LOWEST priority so controllers are checked FIRST
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
        
        System.out.println("🔧 WebMvcConfig: Static resource handlers configured with LOWEST priority");
        System.out.println("   - /static/** -> classpath:/static/, classpath:/public/");
        System.out.println("   - /favicon.ico -> classpath:/");
    }
    
    /**
     * Configure CORS globally
     * Note: Controller-level @CrossOrigin annotations take precedence
     * allowCredentials is not set globally to avoid conflicts with controller-level origins="*"
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .maxAge(3600);
        
        System.out.println("🔧 WebMvcConfig: CORS configured (origin patterns=*, credentials=controller-level)");
    }

    /**
     * DO NOT override configureDefaultServletHandling()
     * For REST APIs, we don't need default servlet handling
     * Enabling it causes issues with routing and requires container-specific servlet names
     */
    // Method removed - let Spring Boot handle this automatically
}
