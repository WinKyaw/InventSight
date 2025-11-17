package com.pos.inventsight.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to ensure all controllers are properly scanned and registered
 */
@Configuration
@ComponentScan(basePackages = {
    "com.pos.inventsight.controller"
})
public class ControllerConfig {
    
    public ControllerConfig() {
        System.out.println("🎯 ControllerConfig initialized - Scanning controllers in com.pos.inventsight.controller");
    }
}
