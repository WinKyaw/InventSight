package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Component
public class ControllerRegistrationLogger implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ControllerRegistrationLogger.class);
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        RequestMappingHandlerMapping handlerMapping = event.getApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);
        
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
                !mappingStr.contains("/api/warehouse-inventory")) {
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
