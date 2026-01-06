package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * Component that logs all registered controller endpoints on application startup.
 * This helps verify that warehouse and sales controllers are properly registered
 * and not being overridden by static resource handlers.
 * 
 * Fixed to handle multiple RequestMappingHandlerMapping beans (e.g., when Actuator 
 * is enabled, controllerEndpointHandlerMapping is also present).
 */
@Component
public class ControllerRegistrationLogger implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ControllerRegistrationLogger.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Get all RequestMappingHandlerMapping beans to avoid NoUniqueBeanDefinitionException
        Map<String, RequestMappingHandlerMapping> mappings = 
                applicationContext.getBeansOfType(RequestMappingHandlerMapping.class);
        
        // Try to get the primary web MVC handler mapping by name
        RequestMappingHandlerMapping handlerMapping = mappings.get("requestMappingHandlerMapping");
        
        if (handlerMapping == null && !mappings.isEmpty()) {
            // Fallback to first available if primary not found
            handlerMapping = mappings.values().iterator().next();
            logger.warn("Primary handler mapping bean 'requestMappingHandlerMapping' not found, using first available: {}", 
                    handlerMapping.getClass().getName());
        }
        
        if (handlerMapping == null) {
            logger.warn("No RequestMappingHandlerMapping beans found. Skipping controller registration logging.");
            return;
        }
        
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        
        logger.info("=== REGISTERED CONTROLLER ENDPOINTS ===");
        logger.info("Total endpoints registered: {}", handlerMethods.size());
        logger.info("");
        
        // Log warehouse-related endpoints
        logger.info("--- Warehouse Endpoints ---");
        handlerMethods.forEach((mapping, method) -> {
            if (mapping.toString().contains("/api/warehouses") || 
                mapping.toString().contains("/api/warehouse-inventory")) {
                logger.info("✓ {} -> {}.{}", 
                    mapping, 
                    method.getBeanType().getSimpleName(), 
                    method.getMethod().getName());
            }
        });
        
        // Log store-inventory endpoints
        logger.info("");
        logger.info("--- Store Inventory Endpoints ---");
        handlerMethods.forEach((mapping, method) -> {
            if (mapping.toString().contains("/api/store-inventory")) {
                logger.info("✓ {} -> {}.{}", 
                    mapping, 
                    method.getBeanType().getSimpleName(), 
                    method.getMethod().getName());
            }
        });
        
        // Log sales-related endpoints
        logger.info("");
        logger.info("--- Sales Endpoints ---");
        handlerMethods.forEach((mapping, method) -> {
            if (mapping.toString().contains("/sales/")) {
                logger.info("✓ {} -> {}.{}", 
                    mapping, 
                    method.getBeanType().getSimpleName(), 
                    method.getMethod().getName());
            }
        });
        
        // Log all other API endpoints
        logger.info("");
        logger.info("--- Other API Endpoints ---");
        handlerMethods.forEach((mapping, method) -> {
            String mappingStr = mapping.toString();
            if (mappingStr.contains("/api/") && 
                !mappingStr.contains("/api/warehouses") && 
                !mappingStr.contains("/api/warehouse-inventory") &&
                !mappingStr.contains("/api/store-inventory")) {
                logger.info("✓ {} -> {}.{}", 
                    mapping, 
                    method.getBeanType().getSimpleName(), 
                    method.getMethod().getName());
            }
        });
        
        logger.info("");
        logger.info("=== END CONTROLLER REGISTRATION ===");
    }
}
