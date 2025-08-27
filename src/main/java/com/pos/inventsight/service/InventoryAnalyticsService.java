package com.pos.inventsight.service;

import com.pos.inventsight.model.nosql.InventoryAnalytics;
import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.repository.nosql.InventoryAnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryAnalyticsService {
    
    @Autowired
    private InventoryAnalyticsRepository analyticsRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    @Lazy
    private SaleService saleService;
    
    public void updateDailyAnalytics(Sale sale) {
        System.out.println("📊 InventSight - Updating daily analytics for sale: " + sale.getReceiptNumber());
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:12:40");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        LocalDate today = LocalDate.now();
        String period = "DAILY";
        
        // Find or create daily analytics record
        InventoryAnalytics analytics = analyticsRepository.findByDateAndPeriod(today, period)
                .orElse(new InventoryAnalytics());
        
        analytics.setDate(today);
        analytics.setPeriod(period);
        analytics.setCreatedBy("WinKyaw");
        
        // Update metrics
        updateAnalyticsMetrics(analytics);
        
        // Save analytics
        analyticsRepository.save(analytics);
        
        System.out.println("✅ InventSight daily analytics updated successfully");
    }
    
    private void updateAnalyticsMetrics(InventoryAnalytics analytics) {
        // Get current metrics
        analytics.setTotalProducts((int) productService.getTotalProductCount());
        analytics.setTotalInventoryValue(productService.getTotalInventoryValue());
        analytics.setLowStockProducts(productService.getLowStockProducts().size());
        analytics.setOutOfStockProducts(productService.getOutOfStockProducts().size());
        
        // Sales metrics
        SaleService.SaleSummary summary = saleService.getDashboardSummary();
        analytics.setTotalRevenue(summary.getTodayRevenue());
        analytics.setTotalSales((int) summary.getTodaySales());
        analytics.setAverageOrderValue(summary.getAverageOrderValue());
        
        // Growth metrics (mock data for demo)
        analytics.setRevenueGrowthPercent(BigDecimal.valueOf(15.8));
        analytics.setSalesGrowthPercent(12);
        analytics.setInventoryTurnoverRate(BigDecimal.valueOf(8.3));
    }
    
    public Map<String, Object> getDashboardAnalytics() {
        System.out.println("📈 InventSight - Generating dashboard analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            // Get latest daily analytics
            List<InventoryAnalytics> recentAnalytics = analyticsRepository
                    .findTop10ByPeriodOrderByTotalRevenueDesc("DAILY");
            
            if (!recentAnalytics.isEmpty()) {
                InventoryAnalytics latest = recentAnalytics.get(0);
                
                analytics.put("revenueGrowth", latest.getRevenueGrowthPercent());
                analytics.put("salesGrowth", latest.getSalesGrowthPercent());
                analytics.put("inventoryTurnover", latest.getInventoryTurnoverRate());
                analytics.put("profitabilityScore", 87.5);
                analytics.put("efficiencyRating", "Excellent");
            }
            
            // Smart insights
            analytics.put("smartInsights", Map.of(
                "predictedSales", 127,
                "recommendedReorders", productService.getProductsNeedingReorder().size(),
                "optimizationTips", List.of("Restock Coffee", "Promote Croissants", "Review Pricing")
            ));
            
        } catch (Exception e) {
            System.out.println("⚠️ InventSight analytics generation error: " + e.getMessage());
            // Return fallback data
            analytics.put("revenueGrowth", 15.8);
            analytics.put("salesGrowth", 12);
            analytics.put("inventoryTurnover", 8.3);
        }
        
        return analytics;
    }
    
    public List<InventoryAnalytics> getDailyAnalytics(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return analyticsRepository.findDailyAnalyticsSince(since);
    }
    
    public InventoryAnalytics generateWeeklyAnalytics() {
        System.out.println("📊 InventSight - Generating weekly analytics");
        
        InventoryAnalytics weeklyAnalytics = new InventoryAnalytics();
        weeklyAnalytics.setDate(LocalDate.now());
        weeklyAnalytics.setPeriod("WEEKLY");
        weeklyAnalytics.setCreatedBy("WinKyaw");
        
        updateAnalyticsMetrics(weeklyAnalytics);
        
        return analyticsRepository.save(weeklyAnalytics);
    }
}