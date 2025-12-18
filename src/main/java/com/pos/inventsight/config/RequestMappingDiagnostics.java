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
        
        // Use array to allow modification inside lambda
        final boolean[] foundNavigationPreferences = {false};
        final int[] userPreferencesCount = {0};
        
        handlerMethods.forEach((mapping, method) -> {
            String patterns = mapping.getPathPatternsCondition() != null 
                ? mapping.getPathPatternsCondition().getPatterns().toString()
                : mapping.getPatternsCondition().getPatterns().toString();
            String httpMethods = mapping.getMethodsCondition().getMethods().toString();
            String handler = method.getBeanType().getSimpleName() + "." + method.getMethod().getName() + "()";
            
            logger.info("  {} {} -> {}", httpMethods, patterns, handler);
            
            // Check for navigation preferences endpoint
            if (patterns.contains("/navigation-preferences")) {
                foundNavigationPreferences[0] = true;
            }
            
            // Count UserPreferencesController endpoints
            if (method.getBeanType().getSimpleName().equals("UserPreferencesController")) {
                userPreferencesCount[0]++;
            }
        });
        
        logger.info("================================================================================");
        logger.info("✅ UserPreferencesController endpoints: {}", userPreferencesCount[0]);
        logger.info("✅ Navigation preferences endpoint found: {}", foundNavigationPreferences[0]);
        logger.info("================================================================================");
        
        if (!foundNavigationPreferences[0]) {
            logger.error("❌ CRITICAL: Navigation preferences endpoint NOT registered!");
            logger.error("   Expected: GET /api/user/navigation-preferences");
            logger.error("   Check UserPreferencesController is being scanned");
        }
    }
}
