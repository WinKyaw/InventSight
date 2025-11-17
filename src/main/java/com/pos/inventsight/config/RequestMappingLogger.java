package com.pos.inventsight.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * Logs all registered request mappings at application startup
 * Helps diagnose routing issues
 */
@Component
public class RequestMappingLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final RequestMappingHandlerMapping handlerMapping;

    public RequestMappingLogger(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 REGISTERED REQUEST MAPPINGS");
        System.out.println("=".repeat(80));
        
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();
        
        int warehouseCount = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();
            
            String patterns = info.getPatternsCondition() != null ? 
                info.getPatternsCondition().toString() : "No patterns";
            String httpMethods = info.getMethodsCondition() != null ? 
                info.getMethodsCondition().toString() : "All methods";
            
            System.out.println(String.format("  %s %s -> %s.%s()",
                httpMethods,
                patterns,
                method.getBeanType().getSimpleName(),
                method.getMethod().getName()));
            
            // Count warehouse-related mappings
            if (patterns.contains("warehouse")) {
                warehouseCount++;
            }
        }
        
        System.out.println("=".repeat(80));
        System.out.println(String.format("✅ Total mappings: %d | Warehouse-related: %d", 
            mappings.size(), warehouseCount));
        
        if (warehouseCount == 0) {
            System.out.println("❌ WARNING: NO WAREHOUSE CONTROLLER MAPPINGS FOUND!");
            System.out.println("   Check that WarehouseController is in the correct package");
            System.out.println("   and is being scanned by Spring");
        }
        
        System.out.println("=".repeat(80) + "\n");
    }
}
