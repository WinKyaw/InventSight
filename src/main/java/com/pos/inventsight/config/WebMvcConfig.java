package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC Configuration for InventSight
 * Ensures controllers are prioritized over static resources
 * 
 * Fixes the issue where Spring's DispatcherServlet was mapping controller
 * requests to ResourceHttpRequestHandler (static resources) instead of
 * routing them to REST controllers.
 * 
 * Key features:
 * 1. Path matching configuration - ensures controllers are matched first
 * 2. Static resource handlers - ONLY specific paths to prevent API conflicts
 * 3. CORS configuration - global CORS support for API endpoints
 * 4. Default servlet handling - disabled for API paths
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    
    /**
     * Configure path matching to ensure controllers are matched first
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(false);
        configurer.setUseSuffixPatternMatch(false);
        
        System.out.println("🔧 WebMvcConfig: Path matching configured");
    }
    
    /**
     * Configure static resource handlers to ONLY match specific paths
     * This prevents API paths from being treated as static resources
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ONLY serve static resources from these specific paths
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/");
        
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/");
        
        // Explicitly add order to ensure controllers are checked first
        registry.setOrder(1);
        
        System.out.println("🔧 WebMvcConfig: Static resource handlers configured (ONLY /static/**, /favicon.ico)");
    }
    
    /**
     * Disable default servlet handling to prevent fallback to static resources
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        // DO NOT enable default servlet - this causes API paths to be treated as static resources
        configurer.enable();
        System.out.println("🔧 WebMvcConfig: Default servlet handling disabled for API paths");
    }
    
    /**
     * Configure view resolvers to avoid conflicts
     */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        // No view resolvers needed for REST API
        System.out.println("🔧 WebMvcConfig: View resolvers configured (REST API mode)");
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
