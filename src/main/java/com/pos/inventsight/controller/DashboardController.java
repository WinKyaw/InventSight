package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    
    // Constants for limit validation
    private static final int MIN_LIMIT = 1;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 100;
    
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
    
    // ==================== New Dashboard Widget Endpoints ====================
    
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<RevenueDTO> getRevenue(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("💰 InventSight - Fetching revenue for user: " + username);
            
            RevenueDTO revenue = dashboardService.getRevenue(period);
            
            System.out.println("✅ InventSight - Revenue retrieved successfully");
            return ResponseEntity.ok(revenue);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching revenue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<OrdersDTO> getOrders(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📋 InventSight - Fetching orders for user: " + username);
            
            OrdersDTO orders = dashboardService.getOrders(period);
            
            System.out.println("✅ InventSight - Orders retrieved successfully");
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<LowStockDTO> getLowStock(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("⚠️ InventSight - Fetching low stock items for user: " + username);
            
            LowStockDTO lowStock = dashboardService.getLowStock();
            
            System.out.println("✅ InventSight - Low stock items retrieved successfully");
            return ResponseEntity.ok(lowStock);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching low stock items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<ProductStatsDTO> getProducts(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📦 InventSight - Fetching product stats for user: " + username);
            
            ProductStatsDTO productStats = dashboardService.getProductStats();
            
            System.out.println("✅ InventSight - Product stats retrieved successfully");
            return ResponseEntity.ok(productStats);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching product stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<CategoryStatsDTO> getCategories(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🏷️ InventSight - Fetching category stats for user: " + username);
            
            CategoryStatsDTO categoryStats = dashboardService.getCategoryStats();
            
            System.out.println("✅ InventSight - Category stats retrieved successfully");
            return ResponseEntity.ok(categoryStats);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching category stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/inventory-value")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<InventoryValueDTO> getInventoryValue(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("💵 InventSight - Fetching inventory value for user: " + username);
            
            InventoryValueDTO inventoryValue = dashboardService.getInventoryValue();
            
            System.out.println("✅ InventSight - Inventory value retrieved successfully");
            return ResponseEntity.ok(inventoryValue);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching inventory value: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/avg-order-value")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<AvgOrderValueDTO> getAvgOrderValue(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching avg order value for user: " + username);
            
            AvgOrderValueDTO avgOrderValue = dashboardService.getAvgOrderValue(period);
            
            System.out.println("✅ InventSight - Avg order value retrieved successfully");
            return ResponseEntity.ok(avgOrderValue);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching avg order value: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/sales-chart")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<SalesChartDTO> getSalesChart(
            @RequestParam(defaultValue = "MONTHLY") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📈 InventSight - Fetching sales chart for user: " + username);
            
            SalesChartDTO salesChart = dashboardService.getSalesChart(period, startDate, endDate);
            
            System.out.println("✅ InventSight - Sales chart retrieved successfully");
            return ResponseEntity.ok(salesChart);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching sales chart: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/best-performer")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<BestPerformerDTO> getBestPerformer(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🏆 InventSight - Fetching best performer for user: " + username);
            
            BestPerformerDTO bestPerformer = dashboardService.getBestPerformer(period);
            
            System.out.println("✅ InventSight - Best performer retrieved successfully");
            return ResponseEntity.ok(bestPerformer);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching best performer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/recent-orders")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'MANAGER', 'OWNER')")
    public ResponseEntity<RecentOrdersDTO> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📜 InventSight - Fetching recent orders for user: " + username);
            
            // Validate limit to prevent excessive resource consumption
            if (limit < MIN_LIMIT) limit = DEFAULT_LIMIT;
            if (limit > MAX_LIMIT) limit = MAX_LIMIT;
            
            RecentOrdersDTO recentOrders = dashboardService.getRecentOrders(limit);
            
            System.out.println("✅ InventSight - Recent orders retrieved successfully");
            return ResponseEntity.ok(recentOrders);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching recent orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}