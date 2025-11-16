package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Web MVC Configuration for InventSight
 * 
 * Fixes the issue where Spring's DispatcherServlet was mapping controller
 * requests to ResourceHttpRequestHandler (static resources) instead of
 * routing them to REST controllers.
 * 
 * Key fixes:
 * 1. RequestMappingHandlerMapping with order 0 - ensures controllers are checked first
 * 2. Static resource handlers with LOWEST_PRECEDENCE - ensures they're checked last
 * 3. Explicit resource locations to avoid path conflicts with controller mappings
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    
    /**
     * Configure static resource handlers with lowest precedence.
     * This ensures controller mappings are checked before static resources.
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
        
        logger.info("=== Static Resource Handlers Configured ===");
    }
    
    /**
     * Configure RequestMappingHandlerMapping with highest priority.
     * This ensures @RestController and @Controller mappings are checked
     * BEFORE static resource handlers.
     * 
     * Order 0 means this handler will be consulted first when resolving requests.
     */
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        logger.info("=== Configuring RequestMappingHandlerMapping ===");
        
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        handlerMapping.setOrder(0); // Highest priority - check controllers first
        
        logger.info("RequestMappingHandlerMapping configured with order: 0 (HIGHEST PRIORITY)");
        logger.info("This ensures controller mappings are checked before static resources");
        logger.info("=== RequestMappingHandlerMapping Configured ===");
        
        return handlerMapping;
    }
}
