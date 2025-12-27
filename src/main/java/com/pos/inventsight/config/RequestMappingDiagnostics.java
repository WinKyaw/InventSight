package com.pos.inventsight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Diagnostic component to log all registered request mappings at startup.
 * Helps identify routing issues and controller registration problems.
 */
@Component
public class RequestMappingDiagnostics implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestMappingDiagnostics.class);
    
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    public RequestMappingDiagnostics(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        
        logger.info("================================================================================");
        logger.info("📋 REGISTERED REQUEST MAPPINGS ({} total)", handlerMethods.size());
        logger.info("================================================================================");
        
        AtomicBoolean foundNavigationPreferences = new AtomicBoolean(false);
        AtomicInteger userPreferencesCount = new AtomicInteger(0);
        AtomicBoolean foundPredefinedItemsBulkCreate = new AtomicBoolean(false);
        AtomicInteger predefinedItemsCount = new AtomicInteger(0);
        
        handlerMethods.forEach((mapping, method) -> {
            String patterns = extractPatterns(mapping);
            String httpMethods = mapping.getMethodsCondition().getMethods().toString();
            String handler = method.getBeanType().getSimpleName() + "." + method.getMethod().getName() + "()";
            
            logger.info("  {} {} -> {}", httpMethods, patterns, handler);
            
            // Check for navigation preferences endpoint
            if (patterns.contains("/navigation-preferences")) {
                foundNavigationPreferences.set(true);
            }
            
            // Count UserPreferencesController endpoints
            if (method.getBeanType().getSimpleName().equals("UserPreferencesController")) {
                userPreferencesCount.incrementAndGet();
            }
            
            // Check for predefined-items endpoints
            if (patterns.contains("/predefined-items")) {
                predefinedItemsCount.incrementAndGet();
                
                // Check specifically for bulk-create endpoint
                if (patterns.contains("/predefined-items/bulk-create")) {
                    foundPredefinedItemsBulkCreate.set(true);
                }
            }
        });
        
        logger.info("================================================================================");
        logger.info("✅ UserPreferencesController endpoints: {}", userPreferencesCount.get());
        logger.info("✅ Navigation preferences endpoint found: {}", foundNavigationPreferences.get());
        logger.info("✅ PredefinedItemsController endpoints: {}", predefinedItemsCount.get());
        logger.info("✅ Predefined items bulk-create endpoint found: {}", foundPredefinedItemsBulkCreate.get());
        logger.info("================================================================================");
        
        if (!foundNavigationPreferences.get()) {
            logger.error("❌ CRITICAL: Navigation preferences endpoint NOT registered!");
            logger.error("   Expected: GET /api/user/navigation-preferences");
            logger.error("   Check UserPreferencesController is being scanned");
        }
        
        if (!foundPredefinedItemsBulkCreate.get()) {
            logger.error("❌ CRITICAL: Predefined items bulk-create endpoint NOT registered!");
            logger.error("   Expected: POST /api/predefined-items/bulk-create");
            logger.error("   Check PredefinedItemsController is being scanned");
            logger.error("   This may cause 'No static resource predefined-items/bulk-create' error");
        }
    }
    
    /**
     * Extract path patterns from RequestMappingInfo.
     * Handles both PathPatternsCondition (newer) and PatternsCondition (legacy).
     */
    private String extractPatterns(RequestMappingInfo mapping) {
        if (mapping.getPathPatternsCondition() != null) {
            return mapping.getPathPatternsCondition().getPatterns().toString();
        } else {
            return mapping.getPatternsCondition().getPatterns().toString();
        }
    }
}
