package com.pos.inventsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableJpaRepositories(
	basePackages = "com.pos.inventsight.repository",
	excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
		type = org.springframework.context.annotation.FilterType.REGEX,
		pattern = "com\\.pos\\.inventsight\\.repository\\.nosql\\..*"
	)
)
@EnableMongoRepositories(basePackages = "com.pos.inventsight.repository.nosql")
@EnableAsync
@EnableScheduling
@EnableCaching
public class InventSightApplication {

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

}
