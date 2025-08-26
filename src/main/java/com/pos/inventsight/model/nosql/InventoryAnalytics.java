package com.pos.inventsight.model.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "inventory_analytics")
public class InventoryAnalytics {
    
    @Id
    private String id;
    
    private LocalDate date;
    private String period; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    // Inventory Metrics
    private BigDecimal totalInventoryValue;
    private Integer totalProducts;
    private Integer lowStockProducts;
    private Integer outOfStockProducts;
    private BigDecimal averageProductValue;
    
    // Sales Performance
    private BigDecimal totalRevenue;
    private Integer totalSales;
    private BigDecimal averageOrderValue;
    private Integer totalItemsSold;
    
    // Top Performing Products
    private List<TopSellingProduct> topSellingProducts;
    private Map<String, Integer> categoryPerformance;
    private Map<String, BigDecimal> hourlyRevenue;
    
    // Forecasting Data
    private Map<String, Integer> predictedDemand;
    private List<String> reorderRecommendations;
    
    // Growth Metrics
    private BigDecimal revenueGrowthPercent;
    private Integer salesGrowthPercent;
    private BigDecimal inventoryTurnoverRate;
    
    // Employee Performance
    private List<EmployeePerformance> employeePerformance;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy = "WinKyaw";
    
    // Constructors
    public InventoryAnalytics() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Nested Classes
    public static class TopSellingProduct {
        private String productName;
        private String productId;
        private String sku;
        private Integer quantitySold;
        private BigDecimal revenue;
        private String category;
        private BigDecimal profitMargin;
        
        // Constructors, Getters, Setters
        public TopSellingProduct() {}
        
        public TopSellingProduct(String productName, String productId, String sku, 
                               Integer quantitySold, BigDecimal revenue, String category) {
            this.productName = productName;
            this.productId = productId;
            this.sku = sku;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
            this.category = category;
        }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public Integer getQuantitySold() { return quantitySold; }
        public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }
        
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }
    }
    
    public static class EmployeePerformance {
        private String employeeId;
        private String employeeName;
        private Integer salesProcessed;
        private BigDecimal revenueGenerated;
        private Double averageTransactionValue;
        private Double performanceScore;
        
        // Constructors, Getters, Setters
        public EmployeePerformance() {}
        
        public EmployeePerformance(String employeeId, String employeeName, Integer salesProcessed, BigDecimal revenueGenerated) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.salesProcessed = salesProcessed;
            this.revenueGenerated = revenueGenerated;
            this.averageTransactionValue = salesProcessed > 0 ? 
                revenueGenerated.divide(new BigDecimal(salesProcessed), 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0.0;
        }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public Integer getSalesProcessed() { return salesProcessed; }
        public void setSalesProcessed(Integer salesProcessed) { this.salesProcessed = salesProcessed; }
        
        public BigDecimal getRevenueGenerated() { return revenueGenerated; }
        public void setRevenueGenerated(BigDecimal revenueGenerated) { this.revenueGenerated = revenueGenerated; }
        
        public Double getAverageTransactionValue() { return averageTransactionValue; }
        public void setAverageTransactionValue(Double averageTransactionValue) { this.averageTransactionValue = averageTransactionValue; }
        
        public Double getPerformanceScore() { return performanceScore; }
        public void setPerformanceScore(Double performanceScore) { this.performanceScore = performanceScore; }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }
    
    public Integer getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Integer totalProducts) { this.totalProducts = totalProducts; }
    
    public Integer getLowStockProducts() { return lowStockProducts; }
    public void setLowStockProducts(Integer lowStockProducts) { this.lowStockProducts = lowStockProducts; }
    
    public Integer getOutOfStockProducts() { return outOfStockProducts; }
    public void setOutOfStockProducts(Integer outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }
    
    public BigDecimal getAverageProductValue() { return averageProductValue; }
    public void setAverageProductValue(BigDecimal averageProductValue) { this.averageProductValue = averageProductValue; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Integer getTotalSales() { return totalSales; }
    public void setTotalSales(Integer totalSales) { this.totalSales = totalSales; }
    
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    
    public Integer getTotalItemsSold() { return totalItemsSold; }
    public void setTotalItemsSold(Integer totalItemsSold) { this.totalItemsSold = totalItemsSold; }
    
    public List<TopSellingProduct> getTopSellingProducts() { return topSellingProducts; }
    public void setTopSellingProducts(List<TopSellingProduct> topSellingProducts) { this.topSellingProducts = topSellingProducts; }
    
    public Map<String, Integer> getCategoryPerformance() { return categoryPerformance; }
    public void setCategoryPerformance(Map<String, Integer> categoryPerformance) { this.categoryPerformance = categoryPerformance; }
    
    public Map<String, BigDecimal> getHourlyRevenue() { return hourlyRevenue; }
    public void setHourlyRevenue(Map<String, BigDecimal> hourlyRevenue) { this.hourlyRevenue = hourlyRevenue; }
    
    public Map<String, Integer> getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(Map<String, Integer> predictedDemand) { this.predictedDemand = predictedDemand; }
    
    public List<String> getReorderRecommendations() { return reorderRecommendations; }
    public void setReorderRecommendations(List<String> reorderRecommendations) { this.reorderRecommendations = reorderRecommendations; }
    
    public BigDecimal getRevenueGrowthPercent() { return revenueGrowthPercent; }
    public void setRevenueGrowthPercent(BigDecimal revenueGrowthPercent) { this.revenueGrowthPercent = revenueGrowthPercent; }
    
    public Integer getSalesGrowthPercent() { return salesGrowthPercent; }
    public void setSalesGrowthPercent(Integer salesGrowthPercent) { this.salesGrowthPercent = salesGrowthPercent; }
    
    public BigDecimal getInventoryTurnoverRate() { return inventoryTurnoverRate; }
    public void setInventoryTurnoverRate(BigDecimal inventoryTurnoverRate) { this.inventoryTurnoverRate = inventoryTurnoverRate; }
    
    public List<EmployeePerformance> getEmployeePerformance() { return employeePerformance; }
    public void setEmployeePerformance(List<EmployeePerformance> employeePerformance) { this.employeePerformance = employeePerformance; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}