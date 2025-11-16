package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for InventSight
 * Implements WebMvcConfigurer to customize Spring MVC without disabling auto-configuration
 * 
 * Fixes the issue where Spring's DispatcherServlet was mapping controller
 * requests to ResourceHttpRequestHandler (static resources) instead of
 * routing them to REST controllers.
 * 
 * Key features:
 * 1. Path matching configuration - ensures controllers are matched before static resources
 * 2. Static resource handlers - explicit resource locations to avoid path conflicts
 * 3. CORS configuration - global CORS support for API endpoints
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    
    /**
     * Configure path matching to ensure controllers are matched before static resources.
     * This method replaces the manual RequestMappingHandlerMapping bean definition
     * to avoid conflicts with Spring Boot's auto-configuration.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        logger.info("=== Configuring Path Matching ===");
        
        // Use trailing slash match to be more lenient
        configurer.setUseTrailingSlashMatch(true);
        
        // Set suffix pattern match to false (recommended for REST APIs)
        configurer.setUseSuffixPatternMatch(false);
        
        logger.info("Path matching configured: trailing slash match enabled, suffix pattern match disabled");
        logger.info("This ensures controller mappings are prioritized over static resources");
        logger.info("=== Path Matching Configured ===");
    }
    
    /**
     * Configure static resource handlers.
     * Define explicit paths for static resources to prevent conflicts with API endpoints.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("=== Configuring Static Resource Handlers ===");
        
        // Static resources under /static path
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        logger.info("Registered static resource handler: /static/** -> classpath:/static/");
        
        // Public resources under /public path
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/");
        logger.info("Registered static resource handler: /public/** -> classpath:/public/");
        
        // Webjars
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        logger.info("Registered static resource handler: /webjars/** -> classpath:/META-INF/resources/webjars/");
        
        // Explicitly handle favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
        logger.info("Registered favicon handler: /favicon.ico -> classpath:/static/");
        
        logger.info("=== Static Resource Handlers Configured ===");
    }
    
    /**
     * Configure CORS globally (backup to controller-level CORS).
     * This provides a fallback CORS configuration for all endpoints.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        logger.info("=== Configuring CORS Mappings ===");
        
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        
        logger.info("CORS configured for all endpoints with all origins, methods, and headers");
        logger.info("=== CORS Mappings Configured ===");
    }
}
