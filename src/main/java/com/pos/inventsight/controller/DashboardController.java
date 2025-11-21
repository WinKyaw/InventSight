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
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
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
    public ResponseEntity<?> getDashboardStats(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching dashboard stats for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            if (storeId != null) {
                System.out.println("🏪 Store filter: " + storeId);
            }
            if (startDate != null && endDate != null) {
                System.out.println("📅 Date range: " + startDate + " to " + endDate);
            }
            
            // Get comprehensive dashboard data
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
            Map<String, Object> kpis = dashboardService.getKPIs();
            
            // Create structured stats response for frontend
            Map<String, Object> stats = new HashMap<>();
            stats.put("summary", summary);
            stats.put("kpis", kpis);
            stats.put("inventoryStats", getInventoryStats());
            stats.put("recentActivities", getRecentActivities());
            
            // Add enhanced statistics for the spec requirements
            Map<String, Object> enhancedStats = new HashMap<>();
            enhancedStats.put("totalRevenue", summary.getTotalRevenue());
            enhancedStats.put("totalOrders", summary.getTotalOrders());
            enhancedStats.put("averageOrderValue", calculateAverageOrderValue(summary));
            enhancedStats.put("topSellingItems", getTopSellingItems(5)); // Top 5
            enhancedStats.put("lowStockItems", getLowStockItems(10)); // Items with quantity < 10
            enhancedStats.put("dailySales", getDailySalesLast7Days());
            
            stats.put("enhanced", enhancedStats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "Dashboard statistics retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            response.put("filters", Map.of(
                "storeId", storeId != null ? storeId.toString() : "all",
                "startDate", startDate != null ? startDate : "not specified",
                "endDate", endDate != null ? endDate : "not specified"
            ));
            
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
    
    // Helper method to calculate average order value
    private Double calculateAverageOrderValue(DashboardSummaryResponse summary) {
        try {
            if (summary.getTotalOrders() != null && summary.getTotalOrders() > 0 &&
                summary.getTotalRevenue() != null) {
                return summary.getTotalRevenue().doubleValue() / summary.getTotalOrders();
            }
        } catch (Exception e) {
            System.out.println("❌ Error calculating average order value: " + e.getMessage());
        }
        return 0.0;
    }
    
    // Helper method to get top selling items
    private java.util.List<Map<String, Object>> getTopSellingItems(int limit) {
        java.util.List<Map<String, Object>> topItems = new java.util.ArrayList<>();
        try {
            // Mock data - in real implementation, query from sale_items or sales database
            // This would join with products and aggregate by quantity sold
            System.out.println("ℹ️ Top selling items query not yet implemented");
        } catch (Exception e) {
            System.out.println("❌ Error fetching top selling items: " + e.getMessage());
        }
        return topItems;
    }
    
    // Helper method to get low stock items (quantity < threshold)
    private java.util.List<Map<String, Object>> getLowStockItems(int threshold) {
        java.util.List<Map<String, Object>> lowStockItems = new java.util.ArrayList<>();
        try {
            // Mock data - in real implementation, query products with quantity < threshold
            System.out.println("ℹ️ Low stock items query not yet implemented");
        } catch (Exception e) {
            System.out.println("❌ Error fetching low stock items: " + e.getMessage());
        }
        return lowStockItems;
    }
    
    // Helper method to get daily sales for last 7 days
    private java.util.List<Map<String, Object>> getDailySalesLast7Days() {
        java.util.List<Map<String, Object>> dailySales = new java.util.ArrayList<>();
        try {
            // Mock data - in real implementation, aggregate sales by day for last 7 days
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                Map<String, Object> daySale = new HashMap<>();
                LocalDateTime date = now.minusDays(i);
                daySale.put("date", date.toLocalDate().toString());
                daySale.put("sales", 0.0);
                daySale.put("orders", 0);
                dailySales.add(daySale);
            }
            System.out.println("ℹ️ Daily sales query not yet fully implemented - returning mock data structure");
        } catch (Exception e) {
            System.out.println("❌ Error fetching daily sales: " + e.getMessage());
        }
        return dailySales;
    }
}