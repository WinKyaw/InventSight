package com.pos.inventsight.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 
 * CRITICAL: Static resource handler is configured with LOWEST_PRECEDENCE
 * to ensure API endpoints are matched BEFORE static resources
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    
    @PostConstruct
    public void init() {
        logger.info("=".repeat(80));
        logger.info("🌐 WebMvcConfig initializing...");
        logger.info("=".repeat(80));
    }
    
    /**
     * Configure path matching for controllers
     * Ensures API endpoints are matched exactly without suffix patterns
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        logger.info("🛤️  Configuring path matching...");
        
        // Allow both /api/items and /api/items/ to match the same endpoint
        // This is useful for REST APIs to accept both forms without redirects
        configurer.setUseTrailingSlashMatch(true);
        
        // Disable suffix pattern matching (e.g., /users.json, /users.xml)
        configurer.setUseSuffixPatternMatch(false);
        
        // Use registered suffix pattern match
        configurer.setUseRegisteredSuffixPatternMatch(false);
        
        logger.info("✅ Path matching configured:");
        logger.info("   - Trailing slash: flexible (both /api/items and /api/items/ work)");
        logger.info("   - Suffix pattern: disabled");
        logger.info("   - API endpoints will match exactly");
    }
    
    /**
     * Configure static resource handling
     * CRITICAL: Must have LOWEST priority to not intercept API requests
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("📁 Configuring static resource handlers...");
        
        // ✅ CRITICAL: Set LOWEST precedence so API endpoints are checked FIRST
        // This must be called on the registry BEFORE adding handlers
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
        
        // Serve static resources ONLY from /static/** path
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/")
                .setCachePeriod(3600)
                .resourceChain(true);
        
        // Handle favicon specifically
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/");
        
        logger.info("✅ Static resource handler configured:");
        logger.info("   - Order: LOWEST_PRECEDENCE (API endpoints checked first)");
        logger.info("   - Pattern: /static/**, /favicon.ico");
        logger.info("   - Locations: classpath:/static/, classpath:/public/");
        logger.info("   - Cache: 3600 seconds");
        logger.info("   - /api/** endpoints will NOT be intercepted");
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
        
        logger.info("🔧 WebMvcConfig: CORS configured (origin patterns=*, credentials=controller-level)");
    }

    /**
     * DO NOT override configureDefaultServletHandling()
     * For REST APIs, we don't need default servlet handling
     * Enabling it causes issues with routing and requires container-specific servlet names
     */
    // Method removed - let Spring Boot handle this automatically
}
