package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.repository.sql.SaleRepository;
import com.pos.inventsight.repository.sql.SaleItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private SaleItemRepository saleItemRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    public Map<String, Object> generateDailyReport() {
        System.out.println("📊 InventSight - Generating daily report");
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:17:13");
        System.out.println("👤 Current User's Login: WinKyaw");
        
        Map<String, Object> report = new HashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        
        try {
            // Sales data
            List<Sale> todaySales = saleRepository.findByDateRange(startOfDay, endOfDay);
            BigDecimal todayRevenue = saleRepository.getTotalRevenueByDateRange(startOfDay, endOfDay);
            long todaySalesCount = saleRepository.getSalesCountByDateRange(startOfDay, endOfDay);
            
            report.put("date", LocalDate.now().toString());
            report.put("salesCount", todaySalesCount);
            report.put("totalRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
            report.put("averageOrderValue", todaySalesCount > 0 ? 
                (todayRevenue != null ? todayRevenue.divide(BigDecimal.valueOf(todaySalesCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO) : 
                BigDecimal.ZERO);
            
            // Inventory insights
            report.put("lowStockCount", productService.getLowStockProducts().size());
            report.put("outOfStockCount", productService.getOutOfStockProducts().size());
            report.put("reorderCount", productService.getProductsNeedingReorder().size());
            
            // Top selling products
            List<Object[]> topProducts = saleItemRepository.findTopSellingProducts();
            report.put("topSellingProducts", topProducts);
            
            // System info
            report.put("generatedAt", "2025-08-26 09:17:13");
            report.put("generatedBy", "WinKyaw");
            report.put("system", "InventSight");
            
            // Log report generation
            activityLogService.logActivity(
                null,
                "WinKyaw",
                "REPORT_GENERATED",
                "SYSTEM",
                "Daily report generated for " + LocalDate.now()
            );
            
            System.out.println("✅ InventSight daily report generated successfully");
            System.out.println("   📈 Sales: " + todaySalesCount + " transactions, $" + todayRevenue);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight report generation failed: " + e.getMessage());
            report.put("error", e.getMessage());
            report.put("status", "FAILED");
        }
        
        return report;
    }
    
    public Map<String, Object> generateWeeklyReport() {
        System.out.println("📊 InventSight - Generating weekly report");
        
        Map<String, Object> report = new HashMap<>();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().atTime(23, 59, 59);
        
        try {
            BigDecimal weeklyRevenue = saleRepository.getTotalRevenueByDateRange(startOfWeek, endOfWeek);
            long weeklySalesCount = saleRepository.getSalesCountByDateRange(startOfWeek, endOfWeek);
            
            report.put("period", "weekly");
            report.put("startDate", startOfWeek.toLocalDate().toString());
            report.put("endDate", endOfWeek.toLocalDate().toString());
            report.put("salesCount", weeklySalesCount);
            report.put("totalRevenue", weeklyRevenue != null ? weeklyRevenue : BigDecimal.ZERO);
            report.put("averageOrderValue", weeklySalesCount > 0 ? 
                (weeklyRevenue != null ? weeklyRevenue.divide(BigDecimal.valueOf(weeklySalesCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO) : 
                BigDecimal.ZERO);
            
            // Inventory performance
            report.put("totalProducts", productService.getTotalProductCount());
            report.put("inventoryValue", productService.getTotalInventoryValue());
            
            report.put("generatedAt", "2025-08-26 09:17:13");
            report.put("generatedBy", "WinKyaw");
            report.put("system", "InventSight");
            
            System.out.println("✅ InventSight weekly report generated");
            
        } catch (Exception e) {
            System.out.println("❌ InventSight weekly report generation failed: " + e.getMessage());
            report.put("error", e.getMessage());
        }
        
        return report;
    }
    
    public Map<String, Object> generateInventoryReport() {
        System.out.println("📦 InventSight - Generating inventory report");
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            List<Product> allProducts = productService.getAllActiveProducts();
            List<Product> lowStockProducts = productService.getLowStockProducts();
            List<Product> outOfStockProducts = productService.getOutOfStockProducts();
            List<Product> reorderProducts = productService.getProductsNeedingReorder();
            
            report.put("totalProducts", allProducts.size());
            report.put("lowStockProducts", lowStockProducts);
            report.put("outOfStockProducts", outOfStockProducts);
            report.put("reorderProducts", reorderProducts);
            report.put("inventoryValue", productService.getTotalInventoryValue());
            report.put("categoryDistribution", productService.getCategoryDistribution());
            report.put("supplierDistribution", productService.getSupplierDistribution());
            
            // Health indicators
            report.put("inventoryHealth", Map.of(
                "lowStockPercentage", allProducts.size() > 0 ? (lowStockProducts.size() * 100.0 / allProducts.size()) : 0,
                "outOfStockPercentage", allProducts.size() > 0 ? (outOfStockProducts.size() * 100.0 / allProducts.size()) : 0,
                "overallScore", calculateInventoryScore(allProducts.size(), lowStockProducts.size(), outOfStockProducts.size())
            ));
            
            report.put("generatedAt", "2025-08-26 09:17:13");
            report.put("generatedBy", "WinKyaw");
            report.put("system", "InventSight");
            
            System.out.println("✅ InventSight inventory report generated: " + allProducts.size() + " products analyzed");
            
        } catch (Exception e) {
            System.out.println("❌ InventSight inventory report generation failed: " + e.getMessage());
            report.put("error", e.getMessage());
        }
        
        return report;
    }
    
    private double calculateInventoryScore(int total, int lowStock, int outOfStock) {
        if (total == 0) return 100.0;
        
        double lowStockPenalty = (lowStock * 10.0) / total;
        double outOfStockPenalty = (outOfStock * 20.0) / total;
        
        return Math.max(0, 100.0 - lowStockPenalty - outOfStockPenalty);
    }
}