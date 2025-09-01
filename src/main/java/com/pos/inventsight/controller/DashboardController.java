package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.DashboardSummaryResponse;
import com.pos.inventsight.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    // GET /api/dashboard/summary - Comprehensive dashboard data
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching dashboard summary for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("summary", summary);
            response.put("message", "Dashboard summary retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Dashboard summary retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching dashboard summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch dashboard summary: " + e.getMessage()));
        }
    }
    
    // GET /api/dashboard/kpis - Key Performance Indicators
    @GetMapping("/kpis")
    public ResponseEntity<?> getDashboardKPIs(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📈 InventSight - Fetching KPIs for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            Map<String, Object> kpis = dashboardService.getKPIs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("kpis", kpis);
            response.put("message", "KPIs retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - KPIs retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching KPIs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch KPIs: " + e.getMessage()));
        }
    }
    
    // GET /api/dashboard/stats - Dashboard statistics (frontend compatibility)
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching dashboard stats for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            // Get comprehensive dashboard data
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
            Map<String, Object> kpis = dashboardService.getKPIs();
            
            // Create structured stats response for frontend
            Map<String, Object> stats = new HashMap<>();
            stats.put("summary", summary);
            stats.put("kpis", kpis);
            stats.put("inventoryStats", getInventoryStats());
            stats.put("recentActivities", getRecentActivities());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "Dashboard statistics retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Dashboard stats retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching dashboard stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch dashboard stats: " + e.getMessage()));
        }
    }
    
    // Helper method to get inventory stats
    private Map<String, Object> getInventoryStats() {
        Map<String, Object> inventoryStats = new HashMap<>();
        try {
            // Basic inventory statistics
            inventoryStats.put("totalProducts", 0);
            inventoryStats.put("lowStockItems", 0);
            inventoryStats.put("outOfStockItems", 0);
            inventoryStats.put("totalValue", 0.0);
        } catch (Exception e) {
            System.out.println("❌ Error fetching inventory stats: " + e.getMessage());
        }
        return inventoryStats;
    }
    
    // Helper method to get recent activities
    private Map<String, Object> getRecentActivities() {
        Map<String, Object> activities = new HashMap<>();
        try {
            // Basic recent activities
            activities.put("recentSales", 0);
            activities.put("recentPurchases", 0);
            activities.put("activeUsers", 0);
        } catch (Exception e) {
            System.out.println("❌ Error fetching recent activities: " + e.getMessage());
        }
        return activities;
    }
}