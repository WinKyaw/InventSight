package com.pos.inventsight.service;

import com.pos.inventsight.dto.DashboardSummaryResponse;
import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import com.pos.inventsight.repository.sql.EmployeeRepository;
import com.pos.inventsight.repository.sql.SaleRepository;
import com.pos.inventsight.repository.sql.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    @Lazy
    private InventoryAnalyticsService analyticsService;
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    public DashboardSummaryResponse getDashboardSummary() {
        System.out.println("📊 InventSight - Generating dashboard summary");
        System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
        System.out.println("👤 Current User: WinKyaw");
        
        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        
        try {
            // Basic metrics
            summary.setTotalProducts(productService.getTotalProductCount());
            summary.setTotalCategories(categoryRepository.countActiveCategories());
            summary.setTotalEmployees(employeeRepository.countActiveEmployees());
            summary.setCheckedInEmployees(employeeRepository.countCheckedInEmployees());
            
            // Sales metrics
            Long totalSales = saleRepository.count();
            summary.setTotalOrders(totalSales);
            
            // Revenue - using inventory value as fallback
            BigDecimal totalRevenue = productService.getTotalInventoryValue();
            summary.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Stock alerts
            Long lowStockCount = (long) productService.getLowStockProducts().size();
            summary.setLowStockItems(lowStockCount);
            
            // Get analytics data from InventoryAnalyticsService
            Map<String, Object> analytics = analyticsService.getDashboardAnalytics();
            if (analytics != null) {
                summary.setRevenueGrowth(getDoubleValue(analytics.get("revenueGrowth")));
                summary.setSalesGrowth(getDoubleValue(analytics.get("salesGrowth")));
                summary.setInventoryTurnover(getDoubleValue(analytics.get("inventoryTurnover")));
                summary.setEfficiencyRating((String) analytics.get("efficiencyRating"));
                summary.setProfitabilityScore(getDoubleValue(analytics.get("profitabilityScore")));
                summary.setSmartInsights((Map<String, Object>) analytics.get("smartInsights"));
            }
            
            // Recent activities (last 10)
            List<Map<String, Object>> recentActivities = activityLogRepository
                .findTop10ByOrderByTimestampDesc()
                .stream()
                .map(activity -> {
                    Map<String, Object> activityMap = new HashMap<>();
                    activityMap.put("id", activity.getId());
                    activityMap.put("action", activity.getAction());
                    activityMap.put("entityType", activity.getEntityType());
                    activityMap.put("description", activity.getDescription());
                    activityMap.put("username", activity.getUsername());
                    activityMap.put("timestamp", activity.getTimestamp());
                    return activityMap;
                })
                .collect(Collectors.toList());
            summary.setRecentActivities(recentActivities);
            
            summary.setTimestamp(LocalDateTime.now());
            summary.setSystem("InventSight");
            
            System.out.println("✅ InventSight - Dashboard summary generated successfully");
            System.out.println("📦 Products: " + summary.getTotalProducts());
            System.out.println("💰 Revenue: " + summary.getTotalRevenue());
            System.out.println("📋 Orders: " + summary.getTotalOrders());
            System.out.println("⚠️ Low Stock Items: " + summary.getLowStockItems());
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error generating dashboard summary: " + e.getMessage());
            e.printStackTrace();
            // Return minimal summary with defaults
            summary.setTotalProducts(0L);
            summary.setTotalRevenue(BigDecimal.ZERO);
            summary.setTotalOrders(0L);
            summary.setLowStockItems(0L);
            summary.setTotalEmployees(0L);
            summary.setCheckedInEmployees(0L);
            summary.setTotalCategories(0L);
            summary.setTimestamp(LocalDateTime.now());
            summary.setSystem("InventSight");
        }
        
        return summary;
    }
    
    public Map<String, Object> getKPIs() {
        System.out.println("📈 InventSight - Generating KPIs");
        
        Map<String, Object> kpis = new HashMap<>();
        
        try {
            // Get analytics data
            Map<String, Object> analytics = analyticsService.getDashboardAnalytics();
            
            kpis.put("revenueGrowth", getDoubleValue(analytics.get("revenueGrowth")));
            kpis.put("salesGrowth", getDoubleValue(analytics.get("salesGrowth")));
            kpis.put("inventoryTurnover", getDoubleValue(analytics.get("inventoryTurnover")));
            kpis.put("profitabilityScore", getDoubleValue(analytics.get("profitabilityScore")));
            kpis.put("efficiencyRating", analytics.get("efficiencyRating"));
            
            // Additional KPIs
            kpis.put("totalProducts", productService.getTotalProductCount());
            kpis.put("lowStockAlert", productService.getLowStockProducts().size());
            kpis.put("outOfStockAlert", productService.getOutOfStockProducts().size());
            kpis.put("employeeUtilization", calculateEmployeeUtilization());
            
            kpis.put("timestamp", LocalDateTime.now());
            kpis.put("system", "InventSight");
            
            System.out.println("✅ InventSight - KPIs generated successfully");
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error generating KPIs: " + e.getMessage());
            kpis.put("error", "Unable to generate KPIs: " + e.getMessage());
        }
        
        return kpis;
    }
    
    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private Double calculateEmployeeUtilization() {
        try {
            long totalEmployees = employeeRepository.countActiveEmployees();
            long checkedInEmployees = employeeRepository.countCheckedInEmployees();
            
            if (totalEmployees == 0) return 0.0;
            
            return (double) checkedInEmployees / totalEmployees * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }
}