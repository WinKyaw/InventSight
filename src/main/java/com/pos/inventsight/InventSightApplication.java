package com.pos.inventsight;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

@SpringBootApplication(scanBasePackages = "com.pos.inventsight")
@EnableJpaRepositories(basePackages = "com.pos.inventsight.repository.sql")
@EnableMongoRepositories(basePackages = "com.pos.inventsight.repository.nosql")
@EnableAsync
@EnableScheduling
@EnableCaching
public class InventSightApplication {

	private static final Logger logger = LoggerFactory.getLogger(InventSightApplication.class);

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	public static void main(String[] args) {
		
		System.out.println("🚀 Starting InventSight Backend System...");
        System.out.println("📅 Current DateTime (UTC): 2025-08-26 08:41:42");
        System.out.println("👤 Current User: WinKyaw");
        System.out.println("🎯 System: InventSight - Intelligent Inventory & POS");
        System.out.println("💾 Initializing Hybrid Database System...");
        SpringApplication.run(InventSightApplication.class, args);
        System.out.println("✅ InventSight Backend System Started Successfully!");
        System.out.println("🌐 API Documentation: http://localhost:8080/api/swagger");
        System.out.println("🔍 Health Check: http://localhost:8080/api/health");
	}

	/**
	 * Log all registered endpoints on startup for debugging
	 */
	@PostConstruct
	public void logEndpoints() {
		logger.info("=".repeat(80));
		logger.info("📋 REGISTERED API ENDPOINTS");
		logger.info("=".repeat(80));
		
		Map<RequestMappingInfo, HandlerMethod> map = 
			requestMappingHandlerMapping.getHandlerMethods();
		
		// Log store-inventory endpoints specifically
		logger.info("🏪 Store Inventory Endpoints:");
		map.forEach((info, method) -> {
			Set<String> patterns = info.getPatternValues();
			if (patterns.stream().anyMatch(p -> p.contains("store-inventory"))) {
				logger.info("  {} -> {}.{}()", 
					info, 
					method.getBeanType().getSimpleName(), 
					method.getMethod().getName());
			}
		});
		
		// Log warehouse endpoints (excluding store-warehouse)
		logger.info("🏭 Warehouse Endpoints:");
		map.forEach((info, method) -> {
			Set<String> patterns = info.getPatternValues();
			boolean isWarehouse = patterns.stream().anyMatch(p -> p.contains("warehouse"));
			boolean isNotStore = patterns.stream().noneMatch(p -> p.contains("store"));
			if (isWarehouse && isNotStore) {
				logger.info("  {} -> {}.{}()", 
					info, 
					method.getBeanType().getSimpleName(), 
					method.getMethod().getName());
			}
		});
		
		logger.info("=".repeat(80));
		logger.info("✅ Total endpoints registered: {}", map.size());
		logger.info("=".repeat(80));
	}

}
