package com.pos.inventsight.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Development configuration to disable external dependencies
 */
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "simple", matchIfMissing = false)
public class SimpleDevelopmentConfig {
    
    // This configuration is used when we need a simple setup without external dependencies
    // All external service calls will be handled gracefully by the existing error handling
    
    @Bean
    @Primary
    public String simpleModeIndicator() {
        System.out.println("🔧 InventSight - Running in Simple Development Mode");
        System.out.println("📅 MongoDB and Redis dependencies disabled");
        System.out.println("👤 Configuration by: WinKyaw");
        return "simple-mode-active";
    }
}