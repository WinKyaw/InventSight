package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

/**
 * Validates that all API endpoints are properly registered
 * Detects potential conflicts with static resource handler
 */
@Component
public class StartupValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupValidator.class);
    
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    public StartupValidator(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateApiEndpoints() {
        logger.info("=".repeat(80));
        logger.info("🔍 VALIDATING ALL API ENDPOINTS");
        logger.info("=".repeat(80));
        
        Set<String> apiPaths = new TreeSet<>();
        
        requestMappingHandlerMapping.getHandlerMethods().forEach((info, method) -> {
            info.getPatternValues().forEach(pattern -> {
                apiPaths.add(pattern);
            });
        });
        
        logger.info("✅ Total endpoints registered: {}", apiPaths.size());
        
        // Check for critical endpoints WITHOUT /api/ prefix (context-path adds it at runtime)
        checkCriticalEndpoint(apiPaths, "/predefined-items/bulk-create");
        checkCriticalEndpoint(apiPaths, "/predefined-items/import-csv");
        checkCriticalEndpoint(apiPaths, "/predefined-items/export-csv");
        checkCriticalEndpoint(apiPaths, "/predefined-items");
        checkCriticalEndpoint(apiPaths, "/predefined-items/{id}");
        checkCriticalEndpoint(apiPaths, "/customers");
        checkCriticalEndpoint(apiPaths, "/customers/guest");
        checkCriticalEndpoint(apiPaths, "/customers/{id}");
        
        // List all endpoints
        logger.info("📋 All registered endpoints:");
        apiPaths.forEach(path -> logger.info("   ✓ {}", path));
        
        logger.info("=".repeat(80));
        logger.info("✅ ENDPOINT VALIDATION COMPLETE");
        logger.info("=".repeat(80));
    }
    
    private void checkCriticalEndpoint(Set<String> apiPaths, String endpoint) {
        // Direct match check - works for both parameterized and non-parameterized endpoints
        // Spring MVC stores paths with literal braces like /api/items/{id}
        boolean found = apiPaths.contains(endpoint);
        
        if (found) {
            logger.info("   ✅ CRITICAL: {} is registered", endpoint);
        } else {
            logger.error("   ❌ CRITICAL MISSING: {} NOT registered!", endpoint);
            logger.error("      This endpoint will return 404!");
        }
    }
}
