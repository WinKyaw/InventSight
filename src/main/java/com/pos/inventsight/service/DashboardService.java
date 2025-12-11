package com.pos.inventsight.service;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.SalesOrder;
import com.pos.inventsight.model.sql.SalesOrderItem;
import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import com.pos.inventsight.repository.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
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
    
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    
    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
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
    
    // ==================== New Dashboard Widget Methods ====================
    
    public RevenueDTO getRevenue(String period) {
        LocalDateTime[] dateRange = getDateRangeForPeriod(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        // Get revenue for current period
        BigDecimal totalRevenue = salesOrderItemRepository.calculateRevenueForPeriod(startDate, endDate);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        // Get revenue for previous period
        LocalDateTime[] prevDateRange = getPreviousDateRangeForPeriod(period);
        BigDecimal previousRevenue = salesOrderItemRepository.calculateRevenueForPeriod(
            prevDateRange[0], prevDateRange[1]
        );
        if (previousRevenue == null) previousRevenue = BigDecimal.ZERO;
        
        // Calculate growth percentage
        Double growthPercentage = 0.0;
        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercentage = totalRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .doubleValue();
        }
        
        return new RevenueDTO(totalRevenue, growthPercentage, period, previousRevenue, "USD");
    }
    
    public OrdersDTO getOrders(String period) {
        LocalDateTime[] dateRange = getDateRangeForPeriod(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        // Get total orders for period
        long totalOrders = salesOrderRepository.countByCreatedAtBetween(startDate, endDate);
        
        // Get today's orders
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        long todayOrders = salesOrderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        
        // Get previous period orders for growth calculation
        LocalDateTime[] prevDateRange = getPreviousDateRangeForPeriod(period);
        long previousOrders = salesOrderRepository.countByCreatedAtBetween(
            prevDateRange[0], prevDateRange[1]
        );
        
        // Calculate growth percentage
        Double growthPercentage = 0.0;
        if (previousOrders > 0) {
            growthPercentage = ((double) (totalOrders - previousOrders) / previousOrders) * 100;
        }
        
        return new OrdersDTO(totalOrders, todayOrders, growthPercentage, period);
    }
    
    public LowStockDTO getLowStock() {
        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        
        List<LowStockDTO.LowStockProduct> products = lowStockProducts.stream()
            .map(p -> new LowStockDTO.LowStockProduct(
                p.getId(),
                p.getName(),
                p.getQuantity(),
                p.getLowStockThreshold(),
                p.getSku()
            ))
            .collect(Collectors.toList());
        
        return new LowStockDTO((long) products.size(), products);
    }
    
    public ProductStatsDTO getProductStats() {
        long activeProducts = productRepository.countActiveProducts();
        long inactiveProducts = productRepository.countInactiveProducts();
        long totalProducts = activeProducts + inactiveProducts;
        
        return new ProductStatsDTO(totalProducts, activeProducts, inactiveProducts);
    }
    
    public CategoryStatsDTO getCategoryStats() {
        long totalCategories = categoryRepository.countActiveCategories();
        
        List<Object[]> topCategoriesData = productRepository.getTopCategoriesByProductCount();
        List<CategoryStatsDTO.TopCategory> topCategories = topCategoriesData.stream()
            .limit(10)
            .map(row -> new CategoryStatsDTO.TopCategory(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
        
        return new CategoryStatsDTO(totalCategories, topCategories);
    }
    
    public InventoryValueDTO getInventoryValue() {
        BigDecimal totalValue = productRepository.getTotalInventoryValue();
        if (totalValue == null) totalValue = BigDecimal.ZERO;
        
        return new InventoryValueDTO(totalValue, "USD", LocalDateTime.now());
    }
    
    public AvgOrderValueDTO getAvgOrderValue(String period) {
        LocalDateTime[] dateRange = getDateRangeForPeriod(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        BigDecimal totalRevenue = salesOrderItemRepository.calculateRevenueForPeriod(startDate, endDate);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        long totalOrders = salesOrderRepository.countByCreatedAtBetween(startDate, endDate);
        
        BigDecimal avgOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            avgOrderValue = totalRevenue.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP);
        }
        
        return new AvgOrderValueDTO(avgOrderValue, "USD", period, totalOrders);
    }
    
    public SalesChartDTO getSalesChart(String period, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start;
        LocalDateTime end;
        
        if (startDate != null && endDate != null) {
            start = startDate.atStartOfDay();
            end = endDate.atTime(LocalTime.MAX);
        } else {
            LocalDateTime[] dateRange = getDateRangeForPeriod(period);
            start = dateRange[0];
            end = dateRange[1];
        }
        
        List<Object[]> chartData = salesOrderItemRepository.getSalesChartData(start, end);
        
        List<SalesChartDTO.DataPoint> dataPoints = chartData.stream()
            .map(row -> {
                LocalDate date = (LocalDate) row[0];
                BigDecimal revenue = (BigDecimal) row[1];
                Long orders = ((Number) row[2]).longValue();
                String label = formatDateLabel(date, period);
                
                return new SalesChartDTO.DataPoint(date, revenue, orders, label);
            })
            .collect(Collectors.toList());
        
        // Calculate summary
        BigDecimal totalRevenue = dataPoints.stream()
            .map(SalesChartDTO.DataPoint::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Long totalOrders = dataPoints.stream()
            .map(SalesChartDTO.DataPoint::getOrders)
            .reduce(0L, Long::sum);
        
        BigDecimal avgOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            avgOrderValue = totalRevenue.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP);
        }
        
        // Calculate growth based on first and last data points
        Double growthPercentage = 0.0;
        if (dataPoints.size() >= 2) {
            BigDecimal firstRevenue = dataPoints.get(0).getRevenue();
            BigDecimal lastRevenue = dataPoints.get(dataPoints.size() - 1).getRevenue();
            
            if (firstRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthPercentage = lastRevenue.subtract(firstRevenue)
                    .divide(firstRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .doubleValue();
            }
        }
        
        SalesChartDTO.Summary summary = new SalesChartDTO.Summary(
            totalRevenue, totalOrders, avgOrderValue, growthPercentage
        );
        
        return new SalesChartDTO(period, dataPoints, summary);
    }
    
    public BestPerformerDTO getBestPerformer(String period) {
        LocalDateTime[] dateRange = getDateRangeForPeriod(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        List<Object[]> performers = salesOrderItemRepository.findBestPerformingProducts(startDate, endDate);
        
        if (performers.isEmpty()) {
            return new BestPerformerDTO(null, period);
        }
        
        Object[] topPerformer = performers.get(0);
        UUID productId = (UUID) topPerformer[0];
        String productName = (String) topPerformer[1];
        String sku = (String) topPerformer[2];
        String category = (String) topPerformer[3];
        Long unitsSold = ((Number) topPerformer[4]).longValue();
        BigDecimal revenue = (BigDecimal) topPerformer[5];
        
        BestPerformerDTO.ProductInfo productInfo = new BestPerformerDTO.ProductInfo(
            productId, productName, sku, unitsSold, revenue, category
        );
        
        return new BestPerformerDTO(productInfo, period);
    }
    
    public RecentOrdersDTO getRecentOrders(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<SalesOrder> recentOrders = salesOrderRepository.findRecentOrders(pageRequest);
        
        List<RecentOrdersDTO.OrderInfo> orders = recentOrders.stream()
            .map(order -> {
                // Calculate total amount from items
                List<SalesOrderItem> items = order.getItems();
                BigDecimal totalAmount = items.stream()
                    .map(SalesOrderItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                String orderNumber = "ORD-" + order.getId().toString().substring(0, 8).toUpperCase();
                
                return new RecentOrdersDTO.OrderInfo(
                    order.getId(),
                    orderNumber,
                    totalAmount,
                    order.getStatus().name(),
                    order.getCustomerName(),
                    items.size(),
                    order.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
        
        return new RecentOrdersDTO(orders);
    }
    
    // ==================== Helper Methods ====================
    
    private LocalDateTime[] getDateRangeForPeriod(String period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        
        switch (period.toUpperCase()) {
            case "DAILY":
                start = LocalDate.now().atStartOfDay();
                break;
            case "WEEKLY":
                start = LocalDate.now().minusWeeks(1).atStartOfDay();
                break;
            case "MONTHLY":
            case "THIS_MONTH":
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                break;
            case "YEARLY":
                start = LocalDate.now().withDayOfYear(1).atStartOfDay();
                break;
            default:
                start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }
        
        return new LocalDateTime[]{start, end};
    }
    
    private LocalDateTime[] getPreviousDateRangeForPeriod(String period) {
        LocalDateTime[] currentRange = getDateRangeForPeriod(period);
        LocalDateTime currentStart = currentRange[0];
        LocalDateTime currentEnd = currentRange[1];
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(currentStart, currentEnd);
        
        LocalDateTime previousEnd = currentStart.minusSeconds(1);
        LocalDateTime previousStart = previousEnd.minusDays(daysBetween);
        
        return new LocalDateTime[]{previousStart, previousEnd};
    }
    
    private String formatDateLabel(LocalDate date, String period) {
        switch (period.toUpperCase()) {
            case "DAILY":
                return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
            case "WEEKLY":
                return "Week of " + date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
            case "MONTHLY":
            case "THIS_MONTH":
                return date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case "YEARLY":
                return String.valueOf(date.getYear());
            default:
                return date.toString();
        }
    }
}