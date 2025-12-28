package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateApiEndpoints() {
        logger.info("=".repeat(80));
        logger.info("🔍 VALIDATING ALL API ENDPOINTS");
        logger.info("=".repeat(80));
        
        Set<String> apiPaths = new TreeSet<>();
        
        requestMappingHandlerMapping.getHandlerMethods().forEach((info, method) -> {
            info.getPatternValues().forEach(pattern -> {
                if (pattern.startsWith("/api/")) {
                    apiPaths.add(pattern);
                }
            });
        });
        
        logger.info("✅ Total API endpoints registered: {}", apiPaths.size());
        
        // Check for critical endpoints
        checkCriticalEndpoint(apiPaths, "/api/predefined-items/bulk-create");
        checkCriticalEndpoint(apiPaths, "/api/predefined-items/import-csv");
        checkCriticalEndpoint(apiPaths, "/api/predefined-items/export-csv");
        checkCriticalEndpoint(apiPaths, "/api/predefined-items");
        checkCriticalEndpoint(apiPaths, "/api/predefined-items/{id}");
        checkCriticalEndpoint(apiPaths, "/api/customers");
        checkCriticalEndpoint(apiPaths, "/api/customers/guest");
        checkCriticalEndpoint(apiPaths, "/api/auth/login");
        checkCriticalEndpoint(apiPaths, "/api/auth/signup");
        
        // List all API endpoints
        logger.info("📋 All registered API endpoints:");
        apiPaths.forEach(path -> logger.info("   ✓ {}", path));
        
        logger.info("=".repeat(80));
        logger.info("✅ API ENDPOINT VALIDATION COMPLETE");
        logger.info("=".repeat(80));
    }
    
    private void checkCriticalEndpoint(Set<String> apiPaths, String endpoint) {
        boolean found = apiPaths.stream()
            .anyMatch(path -> path.equals(endpoint) || path.matches(endpoint.replace("{id}", "\\{[^}]+\\}")));
        
        if (found) {
            logger.info("   ✅ CRITICAL: {} is registered", endpoint);
        } else {
            logger.error("   ❌ CRITICAL MISSING: {} NOT registered!", endpoint);
            logger.error("      This endpoint will return 404!");
        }
    }
}
