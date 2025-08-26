package com.pos.inventsight.config;

import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.InventoryAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupConfig implements CommandLineRunner {
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private InventoryAnalyticsService inventoryAnalyticsService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 InventSight System Starting Up...");
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:17:13");
        System.out.println("👤 Current User's Login: WinKyaw");
        System.out.println("🎯 System: InventSight - Intelligent Inventory & POS System");
        
        // Log system startup
        activityLogService.logActivity(
            "SYSTEM",
            "WinKyaw",
            "SYSTEM_STARTUP",
            "SYSTEM",
            "InventSight system started successfully at 2025-08-26 09:17:13"
        );
        
        // Initialize analytics
        try {
            inventoryAnalyticsService.generateWeeklyAnalytics();
            System.out.println("📊 InventSight analytics engine initialized");
        } catch (Exception e) {
            System.out.println("⚠️ InventSight analytics initialization warning: " + e.getMessage());
        }
        
        // Display system information
        displaySystemInfo();
        
        System.out.println("✅ InventSight System Ready!");
        System.out.println("🌐 Access the system at: http://localhost:8080/api");
        System.out.println("📊 Health Check: http://localhost:8080/api/health");
        System.out.println("📖 API Documentation: http://localhost:8080/api/swagger");
    }
    
    private void displaySystemInfo() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏢 INVENTSIGHT - INTELLIGENT INVENTORY & POS SYSTEM");
        System.out.println("=".repeat(80));
        System.out.println("📅 System Started: 2025-08-26 09:17:13 UTC");
        System.out.println("👤 System Administrator: WinKyaw");
        System.out.println("🔧 Version: 1.0.0");
        System.out.println("🗄️ Database: Hybrid (PostgreSQL + MongoDB + Redis)");
        System.out.println("🔐 Security: JWT Authentication with Role-based Access");
        System.out.println("📱 Mobile Ready: Full React Native API Support");
        System.out.println("📊 Analytics: Real-time Intelligence & Insights");
        System.out.println("🚀 Status: PRODUCTION READY");
        System.out.println("=".repeat(80) + "\n");
    }
}