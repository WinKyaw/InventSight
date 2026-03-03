package com.pos.inventsight.service;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.OrderStatus;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleStatus;
import com.pos.inventsight.model.sql.SalesOrder;
import com.pos.inventsight.model.sql.SalesOrderItem;
import com.pos.inventsight.model.sql.TransferRequestStatus;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.nosql.ActivityLogRepository;
import com.pos.inventsight.repository.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    // Constants for stock level calculation
    private static final int CRITICAL_STOCK_DIVISOR = 2;
    
    // Constants for growth calculation precision
    private static final int GROWTH_CALCULATION_SCALE = 4;
    private static final int GROWTH_DISPLAY_SCALE = 1;
    
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
    
    @Autowired
    private SaleItemRepository saleItemRepository;
    
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    
    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StoreRepository storeRepository;

    public DashboardSummaryResponse getDashboardSummary() {
        return getDashboardSummary(null);
    }

    public DashboardSummaryResponse getDashboardSummary(String storeId) {
        System.out.println("📊 ========== DASHBOARD QUERY DEBUG ==========");
        System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
        System.out.println("👤 Current User: WinKyaw");
        
        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        
        // Define active statuses (including PENDING)
        List<SaleStatus> activeStatuses = Arrays.asList(
            SaleStatus.PENDING,
            SaleStatus.PAID,
            SaleStatus.COMPLETED,
            SaleStatus.DELIVERED,
            SaleStatus.READY_FOR_PICKUP,
            SaleStatus.OUT_FOR_DELIVERY
        );
        
        System.out.println("🔍 Querying sales with statuses: " + activeStatuses);
        
        try {
            // ========== CHECK TOTAL COUNT FIRST ==========
            System.out.println("🔍 Checking total sales count (no filter)...");
            Long allSalesCount = saleRepository.count();
            System.out.println("📊 Total sales in database: " + allSalesCount);
            
            // ========== CHECK PRODUCT COUNT ==========
            System.out.println("🔍 Checking total products count...");
            Long allProductsCount = productRepository.count();
            System.out.println("📦 Total products in database: " + allProductsCount);

            // Resolve store if storeId is provided (must be done before order/revenue queries)
            Store resolvedStore = null;
            if (storeId != null && !storeId.isBlank()) {
                try {
                    resolvedStore = storeRepository.findById(UUID.fromString(storeId)).orElse(null);
                } catch (IllegalArgumentException e) {
                    System.out.println("⚠️ Invalid storeId format: " + storeId + ", falling back to default store resolution");
                }
                System.out.println("🏪 Resolved store for storeId " + storeId + ": " + (resolvedStore != null ? resolvedStore.getStoreName() : "not found"));
            }
            
            // ========== NOW TRY FILTERED QUERY ==========
            System.out.println("🔍 Calling countByStatusIn() with statuses...");
            Long totalSales = null;
            try {
                if (resolvedStore != null) {
                    totalSales = saleRepository.countByStoreAndStatusIn(resolvedStore, activeStatuses);
                } else {
                    totalSales = saleRepository.countByStatusIn(activeStatuses);
                }
                System.out.println("✅ countByStatusIn() returned: " + totalSales);
            } catch (Exception e) {
                System.out.println("❌ ERROR in countByStatusIn(): " + e.getMessage());
                e.printStackTrace();
                totalSales = 0L;
            }
            
            // ========== CHECK REVENUE ==========
            System.out.println("🔍 Calling getTotalRevenueByStatuses()...");
            BigDecimal totalRevenue = null;
            try {
                if (resolvedStore != null) {
                    totalRevenue = saleRepository.getTotalRevenueByStoreAndStatuses(resolvedStore, activeStatuses);
                } else {
                    totalRevenue = saleRepository.getTotalRevenueByStatuses(activeStatuses);
                }
                System.out.println("✅ getTotalRevenueByStatuses() returned: " + totalRevenue);
            } catch (Exception e) {
                System.out.println("❌ ERROR in getTotalRevenueByStatuses(): " + e.getMessage());
                e.printStackTrace();
                totalRevenue = BigDecimal.ZERO;
            }
            
            summary.setTotalOrders(totalSales);
            summary.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

            // Basic metrics
            Long totalProducts;
            if (resolvedStore != null) {
                totalProducts = productRepository.countActiveProductsByStore(resolvedStore);
            } else {
                totalProducts = productService.getTotalProductCount();
            }
            System.out.println("📦 getTotalProductCount() returned: " + totalProducts);
            summary.setTotalProducts(totalProducts);
            summary.setTotalCategories(categoryRepository.countActiveCategories());
            summary.setTotalEmployees(employeeRepository.countActiveEmployees());
            summary.setCheckedInEmployees(employeeRepository.countCheckedInEmployees());
            
            // Stock alerts
            Long lowStockCount;
            if (resolvedStore != null) {
                lowStockCount = (long) productRepository.findLowStockProductsByStore(resolvedStore).size();
            } else {
                lowStockCount = (long) productService.getLowStockProducts().size();
            }
            summary.setLowStockItems(lowStockCount);
            
            // Transfer statistics
            Map<String, Object> transferStats = getTransferStatistics();
            summary.setTransferStats(transferStats);
            
            // Warehouse statistics
            Map<String, Object> warehouseStats = getWarehouseStatistics();
            summary.setWarehouseStats(warehouseStats);
            
            // Sales Order statistics
            Long totalSalesOrders = salesOrderRepository.count();
            BigDecimal salesOrderRevenue = calculateSalesOrderRevenue();
            Map<String, Object> salesOrderStats = new HashMap<>();
            salesOrderStats.put("totalOrders", totalSalesOrders);
            salesOrderStats.put("totalRevenue", salesOrderRevenue);
            summary.setSalesOrderStats(salesOrderStats);
            
            // Combined metrics
            BigDecimal posRevenue = summary.getTotalRevenue();
            summary.setTotalCombinedRevenue(posRevenue.add(salesOrderRevenue));
            summary.setTotalCombinedOrders(totalSales + totalSalesOrders);
            
            System.out.println("⚠️ Low Stock Items: " + summary.getLowStockItems());
            System.out.println("🔄 Total Transfers: " + transferStats.get("totalTransfers"));
            System.out.println("🏭 Total Warehouses: " + warehouseStats.get("totalWarehouses"));
            System.out.println("💰 TOTAL COMBINED REVENUE: $" + summary.getTotalCombinedRevenue());
            System.out.println("📋 TOTAL COMBINED ORDERS: " + summary.getTotalCombinedOrders());
            
            // Get analytics data
            Map<String, Object> analytics = analyticsService.getDashboardAnalytics();
            if (analytics != null) {
                summary.setRevenueGrowth(getDoubleValue(analytics.get("revenueGrowth")));
                summary.setSalesGrowth(getDoubleValue(analytics.get("salesGrowth")));
                summary.setInventoryTurnover(getDoubleValue(analytics.get("inventoryTurnover")));
                summary.setEfficiencyRating((String) analytics.get("efficiencyRating"));
                summary.setProfitabilityScore(getDoubleValue(analytics.get("profitabilityScore")));
                summary.setSmartInsights((Map<String, Object>) analytics.get("smartInsights"));
            }

            // Revenue growth (month-over-month from real data)
            BigDecimal revenueGrowthBD = calculateRevenueGrowth(resolvedStore);
            summary.setRevenueGrowth(revenueGrowthBD != null ? revenueGrowthBD.doubleValue() : 0.0);

            // Order growth (month-over-month from real data)
            BigDecimal orderGrowthBD = calculateOrderGrowth(resolvedStore);
            summary.setOrderGrowth(orderGrowthBD != null ? orderGrowthBD.doubleValue() : 0.0);

            // Inventory value
            BigDecimal inventoryValue;
            if (resolvedStore != null) {
                inventoryValue = productRepository.getTotalInventoryValueByStore(resolvedStore);
            } else {
                inventoryValue = productRepository.getTotalInventoryValue();
            }
            summary.setInventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO);

            // Daily sales (last 7 days)
            List<com.pos.inventsight.dto.DailySales> dailySalesData = getDailySalesLast7Days(resolvedStore);
            List<Map<String, Object>> dailySalesMaps = (dailySalesData != null ? dailySalesData : Collections.<com.pos.inventsight.dto.DailySales>emptyList()).stream()
                .map(ds -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", ds.getDate().toString());
                    m.put("revenue", ds.getRevenue());
                    m.put("orders", ds.getOrderCount());
                    return m;
                })
                .collect(Collectors.toList());
            summary.setDailySales(dailySalesMaps);

            // Top selling items (top 5)
            List<Object[]> topItems;
            if (resolvedStore != null) {
                topItems = saleItemRepository.findTopSellingProductsByStore(resolvedStore, 5);
            } else {
                topItems = saleItemRepository.findTopSellingProducts(5);
            }
            List<Map<String, Object>> topSellingMaps = (topItems != null ? topItems : Collections.<Object[]>emptyList()).stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", row[0]);
                    m.put("quantity", row[1]);
                    m.put("revenue", row[2]);
                    m.put("category", row[3]);
                    return m;
                })
                .collect(Collectors.toList());
            summary.setTopSellingItems(topSellingMaps);

            // Best performer
            Map<String, Object> bestPerformer = computeBestPerformer(resolvedStore);
            summary.setBestPerformer(bestPerformer != null ? bestPerformer : new HashMap<>());

            // Recent orders
            List<Map<String, Object>> recentOrders = computeRecentOrders(10, resolvedStore);
            summary.setRecentOrders(recentOrders != null ? recentOrders : new ArrayList<>());

            // Customer satisfaction (static placeholder)
            summary.setCustomerSatisfaction(4.5);

            // Recent activities
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
            
            System.out.println("🔍 ========== FINAL RESULTS ==========");
            System.out.println("💰 Revenue: $" + summary.getTotalRevenue());
            System.out.println("📋 Orders: " + summary.getTotalOrders());
            System.out.println("📦 Products: " + summary.getTotalProducts());
            System.out.println("🔍 ===================================");
            
        } catch (Exception e) {
            System.out.println("❌ DASHBOARD ERROR: " + e.getMessage());
            e.printStackTrace();
            
            // Return minimal summary with zeros
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

    /**
     * Get best performing product from POS sales (includes PENDING status)
     */
    private Map<String, Object> computeBestPerformer(Store store) {
        Map<String, Object> bestPerformer = new HashMap<>();
        try {
            List<Object[]> results = (store != null)
                ? saleItemRepository.findBestPerformerByStore(store)
                : saleItemRepository.findBestPerformer();
            if (!results.isEmpty()) {
                Object[] result = results.get(0);
                String productName = (String) result[0];
                Long unitsSold = ((Number) result[1]).longValue();
                BigDecimal revenue = (BigDecimal) result[2];
                bestPerformer.put("productName", productName);
                bestPerformer.put("unitsSold", unitsSold);
                bestPerformer.put("revenue", revenue);
                bestPerformer.put("hasData", true);
                System.out.println("🏆 Best Performer Found: " + productName + " (" + unitsSold + " units sold)");
            } else {
                bestPerformer.put("productName", "No Data Available");
                bestPerformer.put("unitsSold", 0L);
                bestPerformer.put("revenue", BigDecimal.ZERO);
                bestPerformer.put("hasData", false);
                System.out.println("⚠️  No sales data available for best performer");
            }
        } catch (Exception e) {
            logger.error("Error fetching best performer: " + e.getMessage());
            bestPerformer.put("productName", "Error");
            bestPerformer.put("unitsSold", 0L);
            bestPerformer.put("revenue", BigDecimal.ZERO);
            bestPerformer.put("hasData", false);
        }
        return bestPerformer;
    }

    /**
     * Get recent orders from POS sales (includes PENDING status)
     */
    private List<Map<String, Object>> computeRecentOrders(int limit, Store store) {
        List<Map<String, Object>> recentOrders = new ArrayList<>();
        try {
            List<SaleStatus> visibleStatuses = Arrays.asList(
                SaleStatus.PENDING,
                SaleStatus.PAID,
                SaleStatus.COMPLETED,
                SaleStatus.DELIVERED,
                SaleStatus.READY_FOR_PICKUP,
                SaleStatus.OUT_FOR_DELIVERY
            );
            List<Sale> sales = (store != null)
                ? saleRepository.findTop10ByStoreAndStatusInOrderByCreatedAtDesc(store, visibleStatuses)
                : saleRepository.findTop10ByStatusInOrderByCreatedAtDesc(visibleStatuses);
            for (Sale sale : sales) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", sale.getId());
                order.put("receiptNumber", sale.getReceiptNumber());
                order.put("totalAmount", sale.getTotalAmount());
                order.put("status", sale.getStatus().toString());
                order.put("customerName", sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer");
                order.put("createdAt", sale.getCreatedAt());
                order.put("paymentMethod", sale.getPaymentMethod() != null ? sale.getPaymentMethod().toString() : "PENDING");
                recentOrders.add(order);
            }
            System.out.println("📋 Found " + recentOrders.size() + " recent orders");
        } catch (Exception e) {
            logger.error("Error fetching recent orders: " + e.getMessage());
        }
        return recentOrders;
    }

    private Map<String, Object> getTransferStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalTransfers", transferRequestRepository.count());
            stats.put("completedTransfers", transferRequestRepository.countByStatus(TransferRequestStatus.COMPLETED));
            stats.put("deliveredTransfers", transferRequestRepository.countByStatus(TransferRequestStatus.DELIVERED));
            stats.put("pendingTransfers", transferRequestRepository.countByStatus(TransferRequestStatus.PENDING));
            stats.put("inTransitTransfers", transferRequestRepository.countByStatus(TransferRequestStatus.IN_TRANSIT));
            stats.put("recentTransfers", transferRequestRepository.findTop10ByOrderByRequestedAtDesc().size());
        } catch (Exception e) {
            logger.error("Error fetching transfer statistics: " + e.getMessage());
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> getWarehouseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalWarehouses", warehouseRepository.count());
            stats.put("activeWarehouses", warehouseRepository.countByIsActiveTrue());
            stats.put("totalInventoryItems", warehouseInventoryRepository.count());
            Integer totalQty = warehouseInventoryRepository.getTotalQuantityAcrossAllWarehouses();
            stats.put("totalQuantityInWarehouses", totalQty != null ? totalQty : 0);
            stats.put("lowStockInWarehouses", warehouseInventoryRepository.findLowStockItems().size());
        } catch (Exception e) {
            logger.error("Error fetching warehouse statistics: " + e.getMessage());
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private BigDecimal calculateSalesOrderRevenue() {
        try {
            List<SalesOrder> orders = salesOrderRepository.findAll();
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (SalesOrder order : orders) {
                if (order.getStatus() == OrderStatus.CONFIRMED ||
                    order.getStatus() == OrderStatus.FULFILLED) {
                    for (SalesOrderItem item : order.getItems()) {
                        totalRevenue = totalRevenue.add(item.getLineTotal());
                    }
                }
            }
            return totalRevenue;
        } catch (Exception e) {
            logger.error("Error calculating sales order revenue: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public Map<String, Object> getKPIs() {
        
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
        LocalDateTime currentEnd = LocalDateTime.now();
        LocalDateTime previousEnd;
        LocalDateTime previousStart;
        
        switch (period.toUpperCase()) {
            case "DAILY":
                previousEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
                previousStart = LocalDate.now().minusDays(1).atStartOfDay();
                break;
            case "WEEKLY":
                previousEnd = LocalDate.now().minusWeeks(1).atTime(LocalTime.MAX);
                previousStart = LocalDate.now().minusWeeks(2).atStartOfDay();
                break;
            case "MONTHLY":
            case "THIS_MONTH":
                // Get the previous month
                LocalDate now = LocalDate.now();
                LocalDate previousMonth = now.minusMonths(1);
                previousStart = previousMonth.withDayOfMonth(1).atStartOfDay();
                previousEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()).atTime(LocalTime.MAX);
                break;
            case "YEARLY":
                previousEnd = LocalDate.now().withDayOfYear(1).minusDays(1).atTime(LocalTime.MAX);
                previousStart = previousEnd.toLocalDate().withDayOfYear(1).atStartOfDay();
                break;
            default:
                // Default to previous month
                LocalDate nowDefault = LocalDate.now();
                LocalDate prevMonth = nowDefault.minusMonths(1);
                previousStart = prevMonth.withDayOfMonth(1).atStartOfDay();
                previousEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).atTime(LocalTime.MAX);
        }
        
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
    
    // ==================== New Analytics Methods ====================
    
    /**
     * Get top selling products with real sales data
     */
    public List<TopSellingProduct> getTopSellingProducts(int limit) {
        try {
            // Query from sale_items joined with products
            List<Object[]> results = saleItemRepository.findTopSellingProducts(limit);
            
            return results.stream()
                .map(row -> new TopSellingProduct(
                    (String) row[0],  // product name
                    ((Number) row[1]).longValue(),  // quantity sold
                    (BigDecimal) row[2],  // total revenue
                    (String) row[3]   // category
                ))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error fetching top selling products: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Get low stock items with threshold
     */
    public List<LowStockItem> getRealLowStockItems(int threshold) {
        try {
            List<Product> lowStockProducts = productRepository
                .findByQuantityLessThanOrderByQuantityAsc(threshold);
            
            return lowStockProducts.stream()
                .map(p -> new LowStockItem(
                    p.getId(),
                    p.getName(),
                    p.getQuantity(),
                    p.getLowStockThreshold() != null ? p.getLowStockThreshold() : threshold,
                    p.getCategory() != null ? p.getCategory() : "Uncategorized",
                    calculateStockLevel(p.getQuantity(), threshold)
                ))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error fetching low stock items: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Calculate stock level status
     */
    private String calculateStockLevel(int current, int threshold) {
        if (current == 0) return "OUT_OF_STOCK";
        if (current < threshold / CRITICAL_STOCK_DIVISOR) return "CRITICAL";
        if (current < threshold) return "LOW";
        return "NORMAL";
    }
    
    /**
     * Get daily sales for last 7 days with real data
     */
    public List<DailySales> getDailySalesLast7Days() {
        return getDailySalesLast7Days(null);
    }

    /**
     * Get daily sales for last 7 days filtered by store
     */
    public List<DailySales> getDailySalesLast7Days(Store store) {
        List<DailySales> dailySales = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        try {
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                
                // Get sales data for this day
                BigDecimal revenue;
                long orderCount;
                if (store != null) {
                    revenue = saleRepository.getTotalRevenueByStoreAndDateRange(store, startOfDay, endOfDay);
                    orderCount = saleRepository.getSalesCountByStoreAndDateRange(store, startOfDay, endOfDay);
                } else {
                    revenue = saleRepository.getTotalRevenueByDateRange(startOfDay, endOfDay);
                    orderCount = saleRepository.getSalesCountByDateRange(startOfDay, endOfDay);
                }
                
                dailySales.add(new DailySales(
                    date,
                    revenue != null ? revenue : BigDecimal.ZERO,
                    orderCount
                ));
            }
            
            return dailySales;
            
        } catch (Exception e) {
            logger.error("Error fetching daily sales: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Calculate revenue growth percentage
     */
    public BigDecimal calculateRevenueGrowth() {
        return calculateRevenueGrowth(null);
    }

    /**
     * Calculate revenue growth percentage filtered by store
     */
    public BigDecimal calculateRevenueGrowth(Store store) {
        try {
            // Get current month revenue
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDateTime.now();
            BigDecimal currentRevenue;
            BigDecimal previousRevenue;
            
            // Get previous month revenue
            LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
            LocalDateTime endOfPrevMonth = startOfMonth.minusSeconds(1);

            if (store != null) {
                currentRevenue = saleRepository.getTotalRevenueByStoreAndDateRange(store, startOfMonth, endOfMonth);
                previousRevenue = saleRepository.getTotalRevenueByStoreAndDateRange(store, startOfPrevMonth, endOfPrevMonth);
            } else {
                currentRevenue = saleRepository.getTotalRevenueByDateRange(startOfMonth, endOfMonth);
                previousRevenue = saleRepository.getTotalRevenueByDateRange(startOfPrevMonth, endOfPrevMonth);
            }
            
            if (previousRevenue == null || previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            if (currentRevenue == null) {
                currentRevenue = BigDecimal.ZERO;
            }
            
            // Calculate growth percentage
            BigDecimal growth = currentRevenue
                .subtract(previousRevenue)
                .divide(previousRevenue, GROWTH_CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
                
            return growth.setScale(GROWTH_DISPLAY_SCALE, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.error("Error calculating revenue growth: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate order growth percentage
     */
    public BigDecimal calculateOrderGrowth() {
        return calculateOrderGrowth(null);
    }

    /**
     * Calculate order growth percentage filtered by store
     */
    public BigDecimal calculateOrderGrowth(Store store) {
        try {
            // Get current month orders
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDateTime.now();
            long currentOrders;
            long previousOrders;
            
            // Get previous month orders
            LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
            LocalDateTime endOfPrevMonth = startOfMonth.minusSeconds(1);

            if (store != null) {
                currentOrders = saleRepository.getSalesCountByStoreAndDateRange(store, startOfMonth, endOfMonth);
                previousOrders = saleRepository.getSalesCountByStoreAndDateRange(store, startOfPrevMonth, endOfPrevMonth);
            } else {
                currentOrders = saleRepository.getSalesCountByDateRange(startOfMonth, endOfMonth);
                previousOrders = saleRepository.getSalesCountByDateRange(startOfPrevMonth, endOfPrevMonth);
            }
            
            if (previousOrders == 0) {
                return BigDecimal.ZERO;
            }
            
            // Calculate growth percentage
            double growth = ((double)(currentOrders - previousOrders) / previousOrders) * 100;
            return BigDecimal.valueOf(growth).setScale(GROWTH_DISPLAY_SCALE, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.error("Error calculating order growth: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}