package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.security.RoleConstants;
import com.pos.inventsight.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    // Constants for limit validation
    private static final int MIN_LIMIT = 1;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 100;
    
    @Autowired
    private DashboardService dashboardService;
    
    // GET /api/dashboard/summary - Comprehensive dashboard data
    @GetMapping("/summary")
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getDashboardSummary(
            Authentication authentication,
            @RequestParam(required = false) String storeId) {
        try {
            String username = authentication.getName();
            logger.info("📊 Dashboard summary requested by: {}", username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary(storeId);
            
            logger.info("📈 Dashboard Data:");
            logger.info("   - Total Revenue: ${}", summary.getTotalRevenue());
            logger.info("   - Total Orders: {}", summary.getTotalOrders());
            logger.info("   - Avg Order Value: ${}", summary.getAvgOrderValue());
            logger.info("   - Total Products: {}", summary.getTotalProducts());
            logger.info("   - Low Stock Items: {}", summary.getLowStockItems());
            if (summary.getBestPerformer() != null) {
                logger.info("   - Best Performer: {}", summary.getBestPerformer().get("productName"));
            }
            if (summary.getRecentOrders() != null) {
                logger.info("   - Recent Orders: {}", summary.getRecentOrders().size());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("summary", summary);
            response.put("message", "Dashboard summary retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Dashboard summary retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching dashboard summary: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch dashboard summary: " + e.getMessage()));
        }
    }
    
    // POST /api/dashboard/refresh - Clear cache and force refresh
    @PostMapping("/refresh")
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> refreshDashboard(
            Authentication authentication,
            @RequestParam(required = false) String storeId) {
        try {
            String username = authentication.getName();
            logger.info("🔄 Dashboard refresh requested by: {}", username);
            
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary(storeId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            response.put("message", "Dashboard refreshed successfully");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error refreshing dashboard: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to refresh dashboard: " + e.getMessage()));
        }
    }
    
    // GET /api/dashboard/kpis - Key Performance Indicators
    @GetMapping("/kpis")
    @PreAuthorize(RoleConstants.GM_PLUS)
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
    @PreAuthorize(RoleConstants.GM_PLUS)
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
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary(storeId != null ? storeId.toString() : null);
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
        try {
            List<TopSellingProduct> topProducts = dashboardService.getTopSellingProducts(limit);
            
            return topProducts.stream()
                .map(product -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", product.getName());
                    item.put("quantity", product.getQuantitySold());
                    item.put("revenue", product.getTotalRevenue());
                    item.put("category", product.getCategory());
                    return item;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.out.println("❌ Error fetching top selling items: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // Helper method to get low stock items (quantity < threshold)
    private java.util.List<Map<String, Object>> getLowStockItems(int threshold) {
        try {
            List<LowStockItem> lowStockItems = dashboardService.getRealLowStockItems(threshold);
            
            return lowStockItems.stream()
                .map(item -> {
                    Map<String, Object> stockItem = new HashMap<>();
                    stockItem.put("id", item.getId());
                    stockItem.put("name", item.getName());
                    stockItem.put("currentStock", item.getCurrentStock());
                    stockItem.put("minStock", item.getMinStock());
                    stockItem.put("category", item.getCategory());
                    stockItem.put("level", item.getStockLevel());
                    return stockItem;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.out.println("❌ Error fetching low stock items: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // Helper method to get daily sales for last 7 days
    private java.util.List<Map<String, Object>> getDailySalesLast7Days() {
        try {
            List<DailySales> dailySales = dashboardService.getDailySalesLast7Days();
            
            return dailySales.stream()
                .map(day -> {
                    Map<String, Object> daySale = new HashMap<>();
                    daySale.put("date", day.getDate().toString());
                    daySale.put("revenue", day.getRevenue());
                    daySale.put("orders", day.getOrderCount());
                    return daySale;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.out.println("❌ Error fetching daily sales: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // ==================== New Dashboard Widget Endpoints ====================
    
    @GetMapping("/revenue")
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getRevenue(
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getOrders(
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getLowStock(Authentication authentication) {
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getProducts(Authentication authentication) {
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getCategories(Authentication authentication) {
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getInventoryValue(Authentication authentication) {
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getAvgOrderValue(
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getSalesChart(
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getBestPerformer(
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
    @PreAuthorize(RoleConstants.GM_PLUS)
    public ResponseEntity<?> getRecentOrders(
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