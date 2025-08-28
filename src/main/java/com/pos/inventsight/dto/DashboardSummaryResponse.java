package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {
    
    private Long totalProducts;
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long lowStockItems;
    private Long totalEmployees;
    private Long checkedInEmployees;
    private Long totalCategories;
    
    // Analytics data
    private Double revenueGrowth;
    private Double salesGrowth;
    private Double inventoryTurnover;
    private String efficiencyRating;
    private Double profitabilityScore;
    
    // Recent activities
    private List<Map<String, Object>> recentActivities;
    
    // Smart insights
    private Map<String, Object> smartInsights;
    
    private LocalDateTime timestamp;
    private String system;
    
    // Constructors
    public DashboardSummaryResponse() {}
    
    // Getters and Setters
    public Long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    
    public Long getLowStockItems() { return lowStockItems; }
    public void setLowStockItems(Long lowStockItems) { this.lowStockItems = lowStockItems; }
    
    public Long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(Long totalEmployees) { this.totalEmployees = totalEmployees; }
    
    public Long getCheckedInEmployees() { return checkedInEmployees; }
    public void setCheckedInEmployees(Long checkedInEmployees) { this.checkedInEmployees = checkedInEmployees; }
    
    public Long getTotalCategories() { return totalCategories; }
    public void setTotalCategories(Long totalCategories) { this.totalCategories = totalCategories; }
    
    public Double getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(Double revenueGrowth) { this.revenueGrowth = revenueGrowth; }
    
    public Double getSalesGrowth() { return salesGrowth; }
    public void setSalesGrowth(Double salesGrowth) { this.salesGrowth = salesGrowth; }
    
    public Double getInventoryTurnover() { return inventoryTurnover; }
    public void setInventoryTurnover(Double inventoryTurnover) { this.inventoryTurnover = inventoryTurnover; }
    
    public String getEfficiencyRating() { return efficiencyRating; }
    public void setEfficiencyRating(String efficiencyRating) { this.efficiencyRating = efficiencyRating; }
    
    public Double getProfitabilityScore() { return profitabilityScore; }
    public void setProfitabilityScore(Double profitabilityScore) { this.profitabilityScore = profitabilityScore; }
    
    public List<Map<String, Object>> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<Map<String, Object>> recentActivities) { this.recentActivities = recentActivities; }
    
    public Map<String, Object> getSmartInsights() { return smartInsights; }
    public void setSmartInsights(Map<String, Object> smartInsights) { this.smartInsights = smartInsights; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
}