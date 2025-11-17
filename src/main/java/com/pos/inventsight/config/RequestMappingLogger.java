package com.pos.inventsight.config;

import org.springframework.beans.factory.annotation.Qualifier;
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

    public RequestMappingLogger(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
        System.out.println("🔧 RequestMappingLogger initialized with requestMappingHandlerMapping");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 REGISTERED REQUEST MAPPINGS (Controllers)");
        System.out.println("=".repeat(80));
        
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();
        
        int totalCount = 0;
        int warehouseCount = 0;
        int inventoryCount = 0;
        
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();
            
            String patterns = info.getPathPatternsCondition() != null ? 
                info.getPathPatternsCondition().toString() : "No patterns";
            String httpMethods = info.getMethodsCondition() != null ? 
                info.getMethodsCondition().toString() : "[All methods]";
            
            String controllerName = method.getBeanType().getSimpleName();
            String methodName = method.getMethod().getName();
            
            System.out.println(String.format("  %-10s %-40s -> %s.%s()",
                httpMethods,
                patterns,
                controllerName,
                methodName));
            
            totalCount++;
            
            // Count warehouse-related mappings
            if (patterns.toLowerCase().contains("warehouse")) {
                warehouseCount++;
            }
            
            // Count inventory-related mappings
            if (patterns.toLowerCase().contains("inventory")) {
                inventoryCount++;
            }
        }
        
        System.out.println("=".repeat(80));
        System.out.println(String.format("✅ Total controller mappings: %d", totalCount));
        System.out.println(String.format("   - Warehouse-related: %d", warehouseCount));
        System.out.println(String.format("   - Inventory-related: %d", inventoryCount));
        
        if (warehouseCount == 0) {
            System.out.println("\n❌ WARNING: NO WAREHOUSE CONTROLLER MAPPINGS FOUND!");
            System.out.println("   Possible causes:");
            System.out.println("   1. WarehouseController is not in the correct package");
            System.out.println("   2. WarehouseController is not being scanned by Spring");
            System.out.println("   3. WarehouseController is missing @RestController annotation");
            System.out.println("   4. Component scanning excludes the controller package\n");
        } else {
            System.out.println("✅ Warehouse controllers are properly registered!");
        }
        
        System.out.println("=".repeat(80) + "\n");
    }
}
